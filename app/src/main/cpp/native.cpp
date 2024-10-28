#include <jni.h>
#include <string>
#include <regex>

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_aviagerman_MainActivity_validatePriceNative(JNIEnv *env, jobject /* this */, jstring input) {
    const char *nativeString = env->GetStringUTFChars(input, nullptr);
    std::string inputString(nativeString);
    env->ReleaseStringUTFChars(input, nativeString);

    std::regex priceRegex("^\\d+(\\.\\d+)?$");
    bool isValid = std::regex_match(inputString, priceRegex);

    return isValid ? JNI_TRUE : JNI_FALSE;
}