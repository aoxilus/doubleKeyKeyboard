#pragma once

#include "predictor.h"

#include <string>
#include <vector>

class KeyboardEngine {
public:
    explicit KeyboardEngine(const std::string& dataDir);

    char onKey(char letter, bool shift);
    char onDualKey(char a, char b, bool shift);
    char onReplaceLastKey(char letter, bool shift);
    void onBackspace();
    void onSpace();
    void onCommit();

    std::string getComposingText() const;
    std::vector<std::string> getSuggestions(int maxCount) const;
    void applySuggestion(int index);
    void resetComposition();

    void updatePredictions();

    int getLearnedWordCount() const;
    void clearUserDictionary();
    void setLearningEnabled(bool enabled);
    void setLanguage(const std::string& code);
    void setTwoTypeMode(bool enabled);

    void saveUserDictionary();

private:
    Predictor predictor_;
    std::string userDictPath_;
    std::string dictDir_;

    std::string composing_;
    std::vector<std::pair<char, char>> keySequence_;
    std::vector<std::string> sentenceContext_;
    std::vector<std::string> pendingSuggestions_;

    void refreshSuggestions();
    void commitComposingWord();
    void rebuildComposing();
    void loadOpenDictionaries();
    bool isExplicitSequence() const;
    std::string buildLiteralComposing() const;
    static char applyShift(char letter, bool shift);
    static char partnerLetter(char letter);
    void addDoubleKeyCorrections(const std::string& literal,
                                 std::vector<std::string>& out) const;
    static void appendUnique(std::vector<std::string>& out, const std::string& word);
    bool learningEnabled_ = true;
    bool twoTypeMode_ = true;

    static constexpr size_t kMaxKeySequence = 48;
};
