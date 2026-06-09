#include "predictor.h"
#include "builtin_words.h"

#include <algorithm>
#include <cctype>
#include <cstdlib>
#include <fstream>
#include <sstream>

void Predictor::loadBuiltin() {
    trie_.clear();
    trie_.emplace_back();
    words_.clear();
    wordToIndex_.clear();
    bigrams_.clear();

    for (const auto& entry : BuiltinWords::spanish()) {
        insertWord(entry.first, entry.second);
    }
    for (const auto& entry : BuiltinWords::english()) {
        insertWord(entry.first, entry.second / 2);
    }
}

void Predictor::loadDictionaryFile(const std::string& path, int maxLines, int freqScale) {
    std::ifstream in(path);
    if (!in.is_open()) {
        return;
    }

    std::string line;
    int loaded = 0;
    while (std::getline(in, line) && loaded < maxLines) {
        if (line.empty() || line[0] == '#') {
            continue;
        }

        std::string word;
        int freq = 1;
        const auto tab = line.find('\t');
        if (tab != std::string::npos) {
            word = line.substr(0, tab);
            freq = std::max(1, std::atoi(line.substr(tab + 1).c_str()) / freqScale);
        } else {
            std::istringstream iss(line);
            iss >> word >> freq;
            freq = std::max(1, freq / freqScale);
        }

        word = foldAccents(normalize(word));
        if (!isAsciiAlphaWord(word) || word.size() < 2) {
            continue;
        }

        insertWord(word, freq);
        loaded++;
    }
}

void Predictor::loadUser(const std::string& path) {
    std::ifstream in(path);
    if (!in.is_open()) {
        return;
    }

    std::string line;
    while (std::getline(in, line)) {
        if (line.empty()) {
            continue;
        }
        if (line.rfind("BG ", 0) == 0) {
            std::istringstream iss(line.substr(3));
            std::string key;
            int count = 1;
            iss >> key >> count;
            if (!key.empty()) {
                bigrams_[key] += count;
            }
            continue;
        }

        std::istringstream iss(line);
        std::string word;
        int freq = 1;
        iss >> word >> freq;
        if (!word.empty()) {
            insertWord(normalize(word), freq);
        }
    }
}

void Predictor::saveUser(const std::string& path) const {
    std::ofstream out(path);
    if (!out.is_open()) {
        return;
    }

    for (const auto& pair : wordToIndex_) {
        const int idx = pair.second;
        if (idx >= 0 && idx < static_cast<int>(words_.size())) {
            const int freq = words_[idx].frequency;
            if (freq > 0) {
                out << pair.first << ' ' << freq << '\n';
            }
        }
    }

    for (const auto& pair : bigrams_) {
        out << "BG " << pair.first << ' ' << pair.second << '\n';
    }
}

void Predictor::learnWord(const std::string& word) {
    const std::string normalized = normalize(word);
    if (normalized.size() < 2) {
        return;
    }
    insertWord(normalized, 5);
}

void Predictor::learnSequence(const std::vector<std::string>& words) {
    for (const auto& word : words) {
        learnWord(word);
    }
    for (size_t i = 1; i < words.size(); ++i) {
        const std::string key = normalize(words[i - 1]) + ' ' + normalize(words[i]);
        bigrams_[key] += 3;
    }
}

std::vector<std::string> Predictor::suggest(
    const std::string& prefix,
    const std::vector<std::string>& context,
    int maxCount) const {

    std::vector<std::pair<std::string, int>> matches;
    if (prefix.empty()) {
        if (!context.empty()) {
            const std::string prev = normalize(context.back());
            const std::string bigramPrefix = prev + ' ';
            for (const auto& pair : bigrams_) {
                if (pair.first.compare(0, bigramPrefix.size(), bigramPrefix) != 0) {
                    continue;
                }
                const std::string word = pair.first.substr(bigramPrefix.size());
                auto wit = wordToIndex_.find(word);
                if (wit != wordToIndex_.end()) {
                    matches.emplace_back(word, pair.second + words_[wit->second].frequency);
                }
            }
        }
    } else {
        int node = 0;
        for (char ch : prefix) {
            auto it = trie_[node].next.find(ch);
            if (it == trie_[node].next.end()) {
                node = -1;
                break;
            }
            node = it->second;
        }
        if (node >= 0) {
            collectPrefixMatches(node, prefix, matches, std::max(maxCount * 4, 32));
        }
    }

    for (auto& match : matches) {
        match.second = scoreWord(match.first, context);
    }

    std::sort(matches.begin(), matches.end(), [](const auto& a, const auto& b) {
        if (a.second != b.second) {
            return a.second > b.second;
        }
        return a.first < b.first;
    });

    std::vector<std::string> result;
    for (const auto& match : matches) {
        if (static_cast<int>(result.size()) >= maxCount) {
            break;
        }
        if (!match.first.empty()) {
            result.push_back(match.first);
        }
    }
    return result;
}

char Predictor::disambiguate(char a, char b, const std::string& prefix,
                             const std::vector<std::string>& context) const {
    const std::string tryA = prefix + static_cast<char>(std::tolower(static_cast<unsigned char>(a)));
    const std::string tryB = prefix + static_cast<char>(std::tolower(static_cast<unsigned char>(b)));

    const int scoreA = scorePrefix(tryA, context);
    const int scoreB = scorePrefix(tryB, context);

    if (scoreA == scoreB) {
        return a;
    }
    return scoreA > scoreB ? a : b;
}

int Predictor::scorePrefix(const std::string& prefix,
                           const std::vector<std::string>& context) const {
    if (prefix.empty()) {
        return 0;
    }

    int node = 0;
    for (char ch : prefix) {
        auto it = trie_[node].next.find(ch);
        if (it == trie_[node].next.end()) {
            return 0;
        }
        node = it->second;
    }

    std::vector<std::pair<std::string, int>> matches;
    collectPrefixMatches(node, prefix, matches, 12);

    int score = 0;
    for (const auto& match : matches) {
        score += scoreWord(match.first, context);
        if (match.first == prefix) {
            score += match.second * 3;
        }
    }
    return score + languageBoost(prefix);
}

void Predictor::resetToBuiltin() {
    loadBuiltin();
    bigrams_.clear();
}

void Predictor::setLanguage(const std::string& code) {
    languageCode_ = code.empty() ? "auto" : code;
}

bool Predictor::isTriePrefix(const std::string& prefix) const {
    if (prefix.empty()) {
        return true;
    }
    int node = 0;
    for (char ch : prefix) {
        auto it = trie_[node].next.find(ch);
        if (it == trie_[node].next.end()) {
            return false;
        }
        node = it->second;
    }
    return true;
}

int Predictor::languageBoost(const std::string& word) const {
    if (languageCode_ == "auto") {
        return 0;
    }

    static const std::string spanishChars = "áéíóúüñ";
    const bool hasSpanishChar = word.find_first_of(spanishChars) != std::string::npos;

    static const std::vector<std::string> commonEs = {
        "que", "de", "la", "el", "en", "es", "un", "una", "por", "con",
        "para", "como", "pero", "hola", "gracias", "eres", "esta", "esto",
        "muy", "bien", "tambien", "cuando", "donde", "quien", "somos",
    };
    bool isCommonEs = hasSpanishChar;
    if (!isCommonEs) {
        for (const auto& w : commonEs) {
            if (word == w) {
                isCommonEs = true;
                break;
            }
        }
    }

    if (languageCode_ == "es" && isCommonEs) {
        return 40;
    }
    if (languageCode_ == "en" && !isCommonEs && !hasSpanishChar) {
        return 20;
    }
    return 0;
}

std::string Predictor::resolveKeySequence(
    const std::vector<std::pair<char, char>>& keys,
    const std::vector<std::string>& context) const {

    if (keys.empty()) {
        return "";
    }

    constexpr int kBeamWidth = 48;
    std::vector<std::string> beam;
    beam.push_back("");

    for (const auto& key : keys) {
        const char a = static_cast<char>(std::tolower(static_cast<unsigned char>(key.first)));
        const char b = key.second == '\0'
                ? '\0'
                : static_cast<char>(std::tolower(static_cast<unsigned char>(key.second)));

        std::vector<std::pair<std::string, int>> scored;
        scored.reserve(beam.size() * 2);

        for (const auto& built : beam) {
            const std::string withA = built + a;
            if (isTriePrefix(withA)) {
                scored.emplace_back(withA, scorePrefix(withA, context) + languageBoost(withA));
            }
            if (b != '\0') {
                const std::string withB = built + b;
                if (isTriePrefix(withB)) {
                    scored.emplace_back(withB, scorePrefix(withB, context) + languageBoost(withB));
                }
            }
        }

        if (scored.empty()) {
            std::string fallback;
            fallback.reserve(keys.size());
            for (const auto& k : keys) {
                fallback.push_back(static_cast<char>(
                        std::tolower(static_cast<unsigned char>(k.first))));
            }
            return fallback;
        }

        std::sort(scored.begin(), scored.end(), [](const auto& left, const auto& right) {
            if (left.second != right.second) {
                return left.second > right.second;
            }
            return left.first < right.first;
        });

        beam.clear();
        const size_t keep = std::min(scored.size(), static_cast<size_t>(kBeamWidth));
        for (size_t i = 0; i < keep; ++i) {
            beam.push_back(scored[i].first);
        }
    }

    std::string best = beam.front();
    int bestScore = -1;
    for (const auto& candidate : beam) {
        int score = scorePrefix(candidate, context) + languageBoost(candidate);
        auto it = wordToIndex_.find(candidate);
        if (it != wordToIndex_.end()) {
            score += words_[it->second].frequency * 4;
        }
        if (score > bestScore) {
            bestScore = score;
            best = candidate;
        }
    }
    return best;
}

std::vector<std::string> Predictor::resolveKeySequenceCandidates(
    const std::vector<std::pair<char, char>>& keys,
    const std::vector<std::string>& context,
    int maxCount) const {

    std::vector<std::string> result;
    if (keys.empty() || maxCount <= 0) {
        return result;
    }

    constexpr int kBeamWidth = 64;
    std::vector<std::string> beam;
    beam.push_back("");

    for (const auto& key : keys) {
        const char a = static_cast<char>(std::tolower(static_cast<unsigned char>(key.first)));
        const char b = key.second == '\0'
                ? '\0'
                : static_cast<char>(std::tolower(static_cast<unsigned char>(key.second)));

        std::vector<std::pair<std::string, int>> scored;
        for (const auto& built : beam) {
            const std::string withA = built + a;
            if (isTriePrefix(withA)) {
                scored.emplace_back(withA, scorePrefix(withA, context) + languageBoost(withA));
            }
            if (b != '\0') {
                const std::string withB = built + b;
                if (isTriePrefix(withB)) {
                    scored.emplace_back(withB, scorePrefix(withB, context) + languageBoost(withB));
                }
            }
        }

        if (scored.empty()) {
            break;
        }

        std::sort(scored.begin(), scored.end(), [](const auto& left, const auto& right) {
            if (left.second != right.second) {
                return left.second > right.second;
            }
            return left.first < right.first;
        });

        beam.clear();
        const size_t keep = std::min(scored.size(), static_cast<size_t>(kBeamWidth));
        for (size_t i = 0; i < keep; ++i) {
            beam.push_back(scored[i].first);
        }
    }

    std::vector<std::pair<std::string, int>> ranked;
    for (const auto& candidate : beam) {
        int score = scorePrefix(candidate, context) + languageBoost(candidate);
        auto it = wordToIndex_.find(candidate);
        if (it != wordToIndex_.end()) {
            score += words_[it->second].frequency * 6;
        }
        ranked.emplace_back(candidate, score);
    }

    std::sort(ranked.begin(), ranked.end(), [](const auto& left, const auto& right) {
        if (left.second != right.second) {
            return left.second > right.second;
        }
        return left.first < right.first;
    });

    std::unordered_map<std::string, bool> seen;
    for (const auto& entry : ranked) {
        if (seen.count(entry.first)) {
            continue;
        }
        seen[entry.first] = true;
        result.push_back(entry.first);
        if (static_cast<int>(result.size()) >= maxCount) {
            break;
        }
    }
    return result;
}

int Predictor::wordCount() const {
    return static_cast<int>(wordToIndex_.size());
}

bool Predictor::hasWord(const std::string& word) const {
    return wordToIndex_.find(normalize(word)) != wordToIndex_.end();
}

int Predictor::insertWord(const std::string& word, int freqBoost) {
    const std::string normalized = normalize(word);
    if (normalized.empty()) {
        return -1;
    }

    auto existing = wordToIndex_.find(normalized);
    if (existing != wordToIndex_.end()) {
        words_[existing->second].frequency += freqBoost;
        return existing->second;
    }

    int node = 0;
    for (char ch : normalized) {
        auto it = trie_[node].next.find(ch);
        if (it == trie_[node].next.end()) {
            trie_[node].next[ch] = static_cast<int>(trie_.size());
            trie_.emplace_back();
            node = static_cast<int>(trie_.size()) - 1;
        } else {
            node = it->second;
        }
    }

    WordEntry entry;
    entry.frequency = freqBoost;
    const int index = static_cast<int>(words_.size());
    words_.push_back(entry);
    trie_[node].wordIndex = index;
    wordToIndex_[normalized] = index;
    return index;
}

void Predictor::collectPrefixMatches(
    int nodeIndex,
    const std::string& built,
    std::vector<std::pair<std::string, int>>& out,
    int maxCollect) const {

    if (static_cast<int>(out.size()) >= maxCollect) {
        return;
    }

    if (trie_[nodeIndex].wordIndex >= 0) {
        const int idx = trie_[nodeIndex].wordIndex;
        out.emplace_back(built, words_[idx].frequency);
    }

    if (static_cast<int>(out.size()) >= maxCollect) {
        return;
    }

    for (const auto& edge : trie_[nodeIndex].next) {
        collectPrefixMatches(edge.second, built + edge.first, out, maxCollect);
        if (static_cast<int>(out.size()) >= maxCollect) {
            return;
        }
    }
}

int Predictor::scoreWord(const std::string& word, const std::vector<std::string>& context) const {
    auto it = wordToIndex_.find(normalize(word));
    if (it == wordToIndex_.end()) {
        return languageBoost(word);
    }

    int score = words_[it->second].frequency + languageBoost(word);
    if (!context.empty()) {
        const std::string key = normalize(context.back()) + ' ' + normalize(word);
        auto bg = bigrams_.find(key);
        if (bg != bigrams_.end()) {
            score += bg->second * 2;
        }
    }
    return score;
}

std::string Predictor::normalize(const std::string& word) {
    std::string out;
    out.reserve(word.size());
    for (char ch : word) {
        out.push_back(static_cast<char>(std::tolower(static_cast<unsigned char>(ch))));
    }
    return out;
}

bool Predictor::isAsciiAlphaWord(const std::string& word) {
    if (word.empty()) {
        return false;
    }
    for (char ch : word) {
        if (ch < 'a' || ch > 'z') {
            return false;
        }
    }
    return true;
}

std::string Predictor::foldAccents(const std::string& word) {
    std::string out;
    out.reserve(word.size());
    for (size_t i = 0; i < word.size();) {
        const unsigned char c = static_cast<unsigned char>(word[i]);
        if (c < 128) {
            if (std::isalpha(c)) {
                out.push_back(static_cast<char>(std::tolower(c)));
            }
            i++;
            continue;
        }
        if (i + 1 < word.size() && c == 0xC3) {
            const unsigned char c2 = static_cast<unsigned char>(word[i + 1]);
            switch (c2) {
                case 0xA1: case 0xA0: case 0xA4: case 0xA2: case 0xA3:
                    out.push_back('a');
                    break;
                case 0xA9: case 0xA8: case 0xAB: case 0xAA:
                    out.push_back('e');
                    break;
                case 0xAD: case 0xAC: case 0xAF: case 0xAE:
                    out.push_back('i');
                    break;
                case 0xB3: case 0xB2: case 0xB6: case 0xB4: case 0xB5:
                    out.push_back('o');
                    break;
                case 0xBA: case 0xB9: case 0xBC: case 0xBE:
                    out.push_back('u');
                    break;
                case 0xB1:
                    out.push_back('n');
                    break;
                case 0xA7:
                    out.push_back('c');
                    break;
                default:
                    break;
            }
            i += 2;
            continue;
        }
        i++;
    }
    return out.empty() ? normalize(word) : out;
}

std::string Predictor::toLower(char c) {
    return std::string(1, static_cast<char>(std::tolower(static_cast<unsigned char>(c))));
}
