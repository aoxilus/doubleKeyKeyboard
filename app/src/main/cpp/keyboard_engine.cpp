#include "keyboard_engine.h"

#include <algorithm>
#include <cctype>
#include <fstream>

namespace {

bool fileExists(const std::string& path) {
    std::ifstream file(path);
    return file.good();
}

}  // namespace

KeyboardEngine::KeyboardEngine(const std::string& dataDir)
    : userDictPath_(dataDir + "/user_dictionary.txt"),
      dictDir_(dataDir + "/dict") {
    predictor_.loadBuiltin();
    loadOpenDictionaries();
    predictor_.loadUser(userDictPath_);
}

void KeyboardEngine::loadOpenDictionaries() {
    const std::string es = dictDir_ + "/es.txt";
    const std::string en = dictDir_ + "/en.txt";
    const std::string names = dictDir_ + "/names.txt";

    if (fileExists(es)) {
        predictor_.loadDictionaryFile(es, 50000, 1000);
    }
    if (fileExists(en)) {
        predictor_.loadDictionaryFile(en, 50000, 2000);
    }
    if (fileExists(names)) {
        predictor_.loadDictionaryFile(names, 10000, 1);
    }
}

char KeyboardEngine::applyShift(char letter, bool shift) {
    if (shift) {
        return static_cast<char>(std::toupper(static_cast<unsigned char>(letter)));
    }
    return static_cast<char>(std::tolower(static_cast<unsigned char>(letter)));
}

char KeyboardEngine::partnerLetter(char letter) {
    static const char pairs[][2] = {
            {'q', 'w'}, {'e', 'r'}, {'t', 'y'}, {'u', 'i'}, {'o', 'p'},
            {'a', 's'}, {'d', 'f'}, {'g', 'h'}, {'j', 'k'},
            {'z', 'x'}, {'c', 'v'}, {'b', 'n'},
    };
    letter = static_cast<char>(std::tolower(static_cast<unsigned char>(letter)));
    for (const auto& pair : pairs) {
        if (letter == pair[0]) {
            return pair[1];
        }
        if (letter == pair[1]) {
            return pair[0];
        }
    }
    return '\0';
}

void KeyboardEngine::appendUnique(std::vector<std::string>& out, const std::string& word) {
    if (word.empty()) {
        return;
    }
    if (std::find(out.begin(), out.end(), word) == out.end()) {
        out.push_back(word);
    }
}

void KeyboardEngine::addDoubleKeyCorrections(
        const std::string& literal,
        std::vector<std::string>& out) const {
    if (literal.size() < 2) {
        return;
    }

    std::vector<std::string> corrections;
    for (size_t i = 0; i < literal.size(); ++i) {
        const char alt = partnerLetter(literal[i]);
        if (alt == '\0') {
            continue;
        }
        std::string variant = literal;
        variant[i] = alt;
        if (predictor_.hasWord(variant)) {
            appendUnique(corrections, variant);
        }
        for (const auto& word : predictor_.suggest(variant, sentenceContext_, 4)) {
            if (word != literal) {
                appendUnique(corrections, word);
            }
        }
    }

    std::vector<std::string> merged;
    for (const auto& word : corrections) {
        appendUnique(merged, word);
    }
    for (const auto& word : out) {
        if (word != literal) {
            appendUnique(merged, word);
        }
    }
    out = std::move(merged);
}

bool KeyboardEngine::isExplicitSequence() const {
    for (const auto& key : keySequence_) {
        if (key.second != '\0') {
            return false;
        }
    }
    return true;
}

std::string KeyboardEngine::buildLiteralComposing() const {
    std::string out;
    out.reserve(keySequence_.size());
    for (const auto& key : keySequence_) {
        out.push_back(static_cast<char>(
                std::tolower(static_cast<unsigned char>(key.first))));
    }
    return out;
}

void KeyboardEngine::rebuildComposing() {
    if (keySequence_.empty()) {
        composing_.clear();
        return;
    }

    // 2-type / explicit letters: show exactly what you typed (fast, no beam search).
    if (twoTypeMode_ || isExplicitSequence()) {
        composing_ = buildLiteralComposing();
        return;
    }

    pendingSuggestions_ = predictor_.resolveKeySequenceCandidates(
            keySequence_, sentenceContext_, 8);
    if (!pendingSuggestions_.empty()) {
        composing_ = pendingSuggestions_.front();
    } else {
        composing_ = predictor_.resolveKeySequence(keySequence_, sentenceContext_);
    }
}

void KeyboardEngine::updatePredictions() {
    refreshSuggestions();
}

void KeyboardEngine::refreshSuggestions() {
    if (keySequence_.empty()) {
        if (!composing_.empty()) {
            pendingSuggestions_ = predictor_.suggest(composing_, sentenceContext_, 8);
        } else {
            pendingSuggestions_ = predictor_.suggest("", sentenceContext_, 8);
        }
        return;
    }

    if (twoTypeMode_ || isExplicitSequence()) {
        composing_ = buildLiteralComposing();
        pendingSuggestions_ = predictor_.suggest(composing_, sentenceContext_, 12);
        pendingSuggestions_.erase(
                std::remove(pendingSuggestions_.begin(), pendingSuggestions_.end(), composing_),
                pendingSuggestions_.end());
        addDoubleKeyCorrections(composing_, pendingSuggestions_);
        if (pendingSuggestions_.size() > 8) {
            pendingSuggestions_.resize(8);
        }
        return;
    }

    pendingSuggestions_ = predictor_.resolveKeySequenceCandidates(
            keySequence_, sentenceContext_, 8);
}

char KeyboardEngine::onDualKey(char a, char b, bool shift) {
    if (keySequence_.size() >= kMaxKeySequence) {
        return applyShift(a, shift);
    }
    keySequence_.push_back({a, b});
    rebuildComposing();
    if (!composing_.empty()) {
        return applyShift(composing_.back(), shift);
    }
    return applyShift(a, shift);
}

char KeyboardEngine::onReplaceLastKey(char letter, bool shift) {
    letter = static_cast<char>(std::tolower(static_cast<unsigned char>(letter)));
    if (!keySequence_.empty()) {
        keySequence_.back().first = letter;
        keySequence_.back().second = '\0';
    } else {
        keySequence_.push_back({letter, '\0'});
    }
    rebuildComposing();
    return applyShift(letter, shift);
}

char KeyboardEngine::onKey(char letter, bool shift) {
    letter = static_cast<char>(std::tolower(static_cast<unsigned char>(letter)));
    if (keySequence_.size() >= kMaxKeySequence) {
        return applyShift(letter, shift);
    }
    keySequence_.push_back({letter, '\0'});
    rebuildComposing();
    if (!composing_.empty()) {
        return applyShift(composing_.back(), shift);
    }
    return applyShift(letter, shift);
}

void KeyboardEngine::onBackspace() {
    if (!keySequence_.empty()) {
        keySequence_.pop_back();
        rebuildComposing();
    }
}

void KeyboardEngine::onSpace() {
    commitComposingWord();
}

void KeyboardEngine::onCommit() {
    commitComposingWord();
    sentenceContext_.clear();
    pendingSuggestions_.clear();
}

void KeyboardEngine::commitComposingWord() {
    if (composing_.empty()) {
        return;
    }

    if (learningEnabled_) {
        if (!sentenceContext_.empty() && !sentenceContext_.back().empty()) {
            predictor_.learnSequence({sentenceContext_.back(), composing_});
        }
        predictor_.learnWord(composing_);
    }
    sentenceContext_.push_back(composing_);

    composing_.clear();
    keySequence_.clear();
    pendingSuggestions_.clear();
    refreshSuggestions();
}

std::string KeyboardEngine::getComposingText() const {
    return composing_;
}

std::vector<std::string> KeyboardEngine::getSuggestions(int maxCount) const {
    if (!pendingSuggestions_.empty()) {
        std::vector<std::string> out = pendingSuggestions_;
        if (static_cast<int>(out.size()) > maxCount) {
            out.resize(static_cast<size_t>(maxCount));
        }
        return out;
    }
    return predictor_.suggest(composing_, sentenceContext_, maxCount);
}

void KeyboardEngine::applySuggestion(int index) {
    if (index >= 0 && index < static_cast<int>(pendingSuggestions_.size())) {
        composing_ = pendingSuggestions_[static_cast<size_t>(index)];
        keySequence_.clear();
    } else {
        auto suggestions = predictor_.suggest(composing_, sentenceContext_, std::max(index + 1, 3));
        if (index >= 0 && index < static_cast<int>(suggestions.size())) {
            composing_ = suggestions[static_cast<size_t>(index)];
            keySequence_.clear();
            pendingSuggestions_ = suggestions;
        }
    }
}

void KeyboardEngine::resetComposition() {
    composing_.clear();
    keySequence_.clear();
    pendingSuggestions_.clear();
}

void KeyboardEngine::saveUserDictionary() {
    predictor_.saveUser(userDictPath_);
}

int KeyboardEngine::getLearnedWordCount() const {
    return predictor_.wordCount();
}

void KeyboardEngine::clearUserDictionary() {
    predictor_.resetToBuiltin();
    loadOpenDictionaries();
    predictor_.saveUser(userDictPath_);
    composing_.clear();
    keySequence_.clear();
    sentenceContext_.clear();
    pendingSuggestions_.clear();
}

void KeyboardEngine::setLearningEnabled(bool enabled) {
    learningEnabled_ = enabled;
}

void KeyboardEngine::setLanguage(const std::string& code) {
    predictor_.setLanguage(code);
    if (!keySequence_.empty()) {
        rebuildComposing();
    }
}

void KeyboardEngine::setTwoTypeMode(bool enabled) {
    twoTypeMode_ = enabled;
    if (!keySequence_.empty()) {
        rebuildComposing();
    }
}
