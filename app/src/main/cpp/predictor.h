#pragma once

#include <string>
#include <unordered_map>
#include <vector>

class Predictor {
public:
    void loadBuiltin();
    void loadDictionaryFile(const std::string& path, int maxLines, int freqScale);
    void loadUser(const std::string& path);
    void saveUser(const std::string& path) const;

    void learnWord(const std::string& word);
    void learnSequence(const std::vector<std::string>& words);

    std::vector<std::string> suggest(
        const std::string& prefix,
        const std::vector<std::string>& context,
        int maxCount) const;

    char disambiguate(char a, char b, const std::string& prefix,
                      const std::vector<std::string>& context) const;

    std::string resolveKeySequence(
        const std::vector<std::pair<char, char>>& keys,
        const std::vector<std::string>& context) const;

    std::vector<std::string> resolveKeySequenceCandidates(
        const std::vector<std::pair<char, char>>& keys,
        const std::vector<std::string>& context,
        int maxCount) const;

    void setLanguage(const std::string& code);
    void resetToBuiltin();
    int wordCount() const;
    bool hasWord(const std::string& word) const;

private:
    struct WordEntry {
        int frequency = 0;
    };

    struct TrieNode {
        std::unordered_map<char, int> next;
        int wordIndex = -1;
    };

    std::vector<TrieNode> trie_;
    std::vector<WordEntry> words_;
    std::unordered_map<std::string, int> wordToIndex_;

    // bigram: prev + " " + word -> count
    std::unordered_map<std::string, int> bigrams_;

    std::string languageCode_ = "auto";

    int insertWord(const std::string& word, int freqBoost);
    void collectPrefixMatches(int nodeIndex, const std::string& built,
                              std::vector<std::pair<std::string, int>>& out,
                              int maxCollect) const;
    int scoreWord(const std::string& word, const std::vector<std::string>& context) const;
    int scorePrefix(const std::string& prefix, const std::vector<std::string>& context) const;
    bool isTriePrefix(const std::string& prefix) const;
    int languageBoost(const std::string& word) const;
    static bool isAsciiAlphaWord(const std::string& word);
    static std::string foldAccents(const std::string& word);
    static std::string normalize(const std::string& word);
    static std::string toLower(char c);
};
