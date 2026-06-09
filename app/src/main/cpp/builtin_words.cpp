#include "builtin_words.h"

namespace BuiltinWords {

static const std::vector<std::pair<std::string, int>> kSpanish = {
    {"hola", 50}, {"como", 45}, {"estas", 40}, {"gracias", 40}, {"por", 60},
    {"favor", 35}, {"que", 80}, {"para", 70}, {"con", 65}, {"una", 55},
    {"uno", 50}, {"muy", 45}, {"bien", 50}, {"mal", 30}, {"si", 40},
    {"no", 70}, {"yo", 35}, {"tu", 40}, {"el", 75}, {"la", 75},
    {"los", 50}, {"las", 45}, {"de", 90}, {"en", 85}, {"es", 60},
    {"son", 40}, {"ser", 35}, {"estar", 40}, {"tengo", 35}, {"tiene", 35},
    {"hacer", 40}, {"ver", 30}, {"ir", 35}, {"día", 30}, {"año", 30},
    {"casa", 35}, {"trabajo", 35}, {"amigo", 30}, {"amiga", 30},
    {"mensaje", 35}, {"texto", 30}, {"teclado", 25}, {"palabra", 30},
    {"escribir", 30}, {"aprender", 25}, {"predicción", 20},
    {"blackberry", 15}, {"android", 25}, {"teléfono", 30}, {"correo", 30},
    {"reunión", 25}, {"mañana", 30}, {"hoy", 35}, {"ahora", 35},
    {"después", 30}, {"antes", 25}, {"donde", 30}, {"cuando", 30},
    {"porque", 35}, {"pero", 40}, {"también", 35}, {"solo", 30},
    {"nuevo", 25}, {"nueva", 25}, {"bueno", 30}, {"buena", 25},
    {"eres", 55}, {"esta", 50}, {"este", 45}, {"esto", 50}, {"estoy", 45},
    {"somos", 40}, {"quien", 40}, {"qué", 45}, {"cómo", 45},
    {"dónde", 40}, {"cuándo", 40}, {"tú", 45}, {"mí", 35}, {"sí", 45},
    {"aquí", 40}, {"allí", 35}, {"ahora", 35}, {"siempre", 35}, {"nunca", 35},
    {"amor", 35}, {"vida", 40}, {"gente", 35}, {"cosa", 35}, {"tiempo", 40},
    {"nombre", 35}, {"noche", 35}, {"día", 30}, {"años", 30}, {"país", 30},
    {"mundo", 35}, {"ciudad", 30}, {"familia", 35}, {"hermano", 30},
    {"hermana", 30}, {"padre", 30}, {"madre", 30}, {"hijo", 30}, {"hija", 30},
    {"escuela", 30}, {"estudio", 30}, {"libro", 30}, {"leer", 30},
    {"comer", 35}, {"beber", 30}, {"dormir", 30}, {"salir", 30}, {"entrar", 30},
    {"venir", 30}, {"decir", 40}, {"saber", 35}, {"poder", 40}, {"querer", 40},
    {"creo", 40}, {"crees", 40}, {"cree", 35}, {"creemos", 30}, {"pienso", 35},
    {"piensa", 35}, {"voy", 40}, {"vas", 35}, {"va", 40}, {"vamos", 35},
    {"fui", 30}, {"fue", 35}, {"será", 30}, {"está", 45}, {"están", 35},
    {"tengo", 35}, {"tienes", 35}, {"tenemos", 30}, {"hace", 40}, {"haces", 30},
    {"necesito", 35}, {"necesitas", 30}, {"puedo", 40}, {"puedes", 35},
    {"puede", 40}, {"podemos", 30}, {"dime", 35}, {"dices", 30}, {"dice", 40},
    {"llama", 30}, {"llamar", 30}, {"espera", 30}, {"espero", 35},
};

static const std::vector<std::pair<std::string, int>> kEnglish = {
    {"the", 100}, {"and", 90}, {"you", 85}, {"that", 80}, {"for", 75},
    {"are", 70}, {"with", 70}, {"this", 65}, {"have", 65}, {"from", 60},
    {"hello", 50}, {"thanks", 45}, {"please", 45}, {"good", 50}, {"great", 40},
    {"work", 45}, {"home", 40}, {"phone", 40}, {"text", 40}, {"message", 40},
    {"keyboard", 30}, {"word", 35}, {"learn", 30}, {"predict", 25},
    {"blackberry", 20}, {"android", 30}, {"today", 35}, {"tomorrow", 30},
    {"meeting", 30}, {"email", 35}, {"friend", 30}, {"write", 35},
    {"about", 40}, {"would", 35}, {"could", 35}, {"should", 35},
    {"your", 60}, {"will", 55}, {"can", 50}, {"not", 55}, {"but", 50},
};

const std::vector<std::pair<std::string, int>>& spanish() { return kSpanish; }
const std::vector<std::pair<std::string, int>>& english() { return kEnglish; }

}
