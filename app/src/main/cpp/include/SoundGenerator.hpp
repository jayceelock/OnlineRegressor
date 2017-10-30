#ifndef SOUND_GENERATOR
#define SOUND_GENERATOR

#define SOUNDLOG "SoundGenerator.cpp"
#define NUM_BUFFERS 1
#define SOUND_LEN 8
#define SAMPLE_RATE 44100

#include <jni.h>
#include <malloc.h>
#include <math.h>
#include <stdlib.h>
#include <string.h>

#include <android/log.h>

#include <AL/al.h>
#include <AL/alc.h>
#include <AL/alext.h>

namespace SoundGeneratorSpace
{
    class SoundGenerator
    {
    public:
        SoundGenerator();
        ~SoundGenerator();

        // Initialise and kill OpenAL library
        bool init();
        bool kill();

        bool startSound();
        bool endSound();

        // Sound generating functions
        short* generateSoundWave(size_t bufferSize, jfloat pitch, short lastVal, bool onUpSwing);
        void playTarget(JNIEnv* env, jfloatArray src, jfloatArray list, jfloat gain, jfloat pitch);
        void playBand(JNIEnv* env, jfloat offset, jfloat pitch);
        void startPlay(ALuint source, ALuint* buf, jfloat pitch);
        void updatePlay(jfloat pitch);

        // Helper functions
        bool sourcePlaying(ALuint source);

    private:
        ALuint targetSrc;
        ALuint bandSrc;
        ALuint targetBuf[NUM_BUFFERS];
        ALuint bandBuf[NUM_BUFFERS];

        bool targetPlaying = false;
        bool bandPlaying = false;
    };
}

#endif