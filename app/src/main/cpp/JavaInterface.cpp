#include <JavaInterface.hpp>

static SoundGeneratorSpace::SoundGenerator sound;

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT bool JNICALL
Java_com_activis_jaycee_onlineregressor_JNINativeInterface_init(JNIEnv* env, jobject obj)
{
    sound.init();
    sound.startSound();

    return true;
}

JNIEXPORT bool JNICALL
Java_com_activis_jaycee_onlineregressor_JNINativeInterface_kill(JNIEnv* env, jobject obj)
{
    sound.endSound();
    sound.kill();

    return true;
}

JNIEXPORT void JNICALL
Java_com_activis_jaycee_onlineregressor_JNINativeInterface_playTarget(JNIEnv* env, jobject obj, jfloatArray src, jfloatArray list, jfloat gain, jfloat pitch)
{
    sound.playTarget(env, src, list, gain, pitch);

    return;
}

JNIEXPORT void JNICALL
Java_com_activis_jaycee_onlineregressor_JNINativeInterface_playBand(JNIEnv* env, jobject obj, jfloat offset, jfloat pitch)
{
    sound.playBand(env, offset, pitch);

    return;
}

#ifdef __cplusplus
}
#endif
