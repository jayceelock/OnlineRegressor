#include <SoundGenerator.hpp>

namespace SoundGeneratorSpace
{
    SoundGenerator::SoundGenerator() { }
    SoundGenerator::~SoundGenerator() { }

    bool SoundGenerator::init()
    {
        ALCdevice* device;
        ALCcontext* context;

        device = alcOpenDevice(NULL);
        if(!device)
        {
            __android_log_print(ANDROID_LOG_ERROR, SOUNDLOG, "Error opening device.");

            return -1;
        }

        context = alcCreateContext(device, NULL);
        if(context == NULL || alcMakeContextCurrent(context) == ALC_FALSE)
        {
            if(context != NULL)
            {
                alcDestroyContext(context);
            }
            alcCloseDevice(device);
            __android_log_print(ANDROID_LOG_ERROR, SOUNDLOG, "Error creating context.");

            return -1;
        }

        __android_log_print(ANDROID_LOG_INFO, SOUNDLOG, "OpenAL Sound initialised.");

        return 0;
    }

    bool SoundGenerator::kill()
    {
        ALCdevice* device = NULL;
        ALCcontext* context = NULL;

        context = alcGetCurrentContext();
        device = alcGetContextsDevice(context);

        alcMakeContextCurrent(NULL);
        alcDestroyContext(context);
        alcCloseDevice(device);

        __android_log_print(ANDROID_LOG_INFO, SOUNDLOG, "OpenAL Sound destroyed.");

        return 0;
    }

    bool SoundGenerator::startSound()
    {
        alGenBuffers(NUM_BUFFERS, targetBuf);
        alGenBuffers(NUM_BUFFERS, bandBuf);
        alGenSources(1, &targetSrc);
        alGenSources(1, &bandSrc);

        targetPlaying = false;
        bandPlaying = false;

        __android_log_print(ANDROID_LOG_INFO, SOUNDLOG, "Started sound");

        return 0;
    }

    bool SoundGenerator::endSound()
    {
        alDeleteBuffers(NUM_BUFFERS, targetBuf);
        alDeleteBuffers(NUM_BUFFERS, bandBuf);
        alDeleteSources(1, &targetSrc);
        alDeleteSources(1, &bandSrc);

        targetPlaying = false;
        bandPlaying = false;

        __android_log_print(ANDROID_LOG_INFO, SOUNDLOG, "Ended Sound.");

        return 0;
    }

    void SoundGenerator::playTarget(JNIEnv *env, jfloatArray src, jfloatArray list, jfloat gain, jfloat pitch)
    {
        // Get source and listener coords and write to JArray
        jsize srcLen = env->GetArrayLength(src);
        jsize listLen = env->GetArrayLength(list);

        jfloat* lSrc = new float[srcLen];
        jfloat* lList = new float[listLen];

        env->GetFloatArrayRegion(src, 0, srcLen, lSrc);
        env->GetFloatArrayRegion(list, 0, listLen, lList);

        // Set source properties
        alSourcef(targetSrc, AL_GAIN, gain);

        alSourcei(targetSrc, AL_LOOPING, AL_TRUE);

        if(!sourcePlaying(targetSrc))
        {
            startPlay(targetSrc, targetBuf, pitch);
            targetPlaying = true;
        }

        else
        {
            alSourcef(targetSrc, AL_PITCH, pitch / 512.f);
        }
    }

    void SoundGenerator::playBand(JNIEnv* env, jfloat offset, jfloat pitch)
    {
        float gain;
        if(offset <= 0.05)
        {
            gain = -200 * offset*offset + 0.5f;
        }
        else
        {
            gain = 0.f;
        }

        alSourcef(bandSrc, AL_GAIN, gain);
        alSourcei(bandSrc, AL_LOOPING, AL_TRUE);

        if(!sourcePlaying(bandSrc))// || !sourcePlaying(bandSrc[1]))
        {
            startPlay(bandSrc, bandBuf, pitch / 2);
            bandPlaying = true;
        }

        else
        {
            alSourcef(bandSrc, AL_PITCH, pitch / 512.f / 2);
        }

    }

    void SoundGenerator::startPlay(ALuint source, ALuint* buf, jfloat pitch)
    {
        /*
         * 1. Generate buffers
         * 2. Fill buffers
         * 3. Que buffers
         * 4 . Play source
         */

        size_t bufferSize = SOUND_LEN * SAMPLE_RATE / NUM_BUFFERS;
        short lastVal = 0;
        bool onUpSwing = true;

        for(int i = 0; i < NUM_BUFFERS; i ++)
        {
            short* samples = generateSoundWave(bufferSize, pitch, lastVal, onUpSwing);
            lastVal = samples[bufferSize - 1];
            if(lastVal - samples[bufferSize - 2] > 0)
            {
                onUpSwing = true;
            }
            else
            {
                onUpSwing = false;
            }

            alBufferData(buf[i], AL_FORMAT_MONO16, samples, bufferSize, SAMPLE_RATE);
            free(samples);
        }

        alSourceQueueBuffers(source, NUM_BUFFERS, buf);
        alSourcePlay(source);

        __android_log_print(ANDROID_LOG_INFO, SOUNDLOG, "Playing");
    }

    void SoundGenerator::updatePlay(jfloat pitch)
    {
        /*
         * 1. Check processed buffers
         * 2. For each procesed buffer:
         *    - Unqueue buffer
         *    - Load new sound data into buffer
         *    - Requeue buffer
         * 3. Ensure source is playing, restart if needed
         */

        ALuint buffer;
        ALint processedBuffers;

        alGetSourcei(targetSrc, AL_BUFFERS_PROCESSED, &processedBuffers);

        if(processedBuffers < 1)
        {
            // __android_log_print(ANDROID_LOG_INFO, SOUNDLOG, "No buffers to update");

            return;
        }
        __android_log_print(ANDROID_LOG_INFO, SOUNDLOG, "Updating buffers, pitch: %d", (int)pitch);

        size_t bufferSize = SOUND_LEN * SAMPLE_RATE / NUM_BUFFERS;
        static short lastVal = 0;
        static bool onUpSwing = false;

        while(processedBuffers --)
        {
            /* Fill samples before unqueing */
            short* samples = generateSoundWave(bufferSize, pitch, lastVal, onUpSwing);
            alSourceUnqueueBuffers(targetSrc, 1, &buffer);

            alBufferData(buffer, AL_FORMAT_MONO16, samples, bufferSize, SAMPLE_RATE);

            alSourceQueueBuffers(targetSrc, 1, &buffer);

            /* Prep for next update */
            lastVal = samples[bufferSize - 1];
            if(lastVal - samples[bufferSize - 2] > 0)
            {
                onUpSwing = true;
            }
            else
            {
                onUpSwing = false;
            }
            __android_log_print(ANDROID_LOG_INFO, SOUNDLOG, "meh last val: %d", lastVal);

            free(samples);
        }

        if(!sourcePlaying(targetSrc))
        {
            alSourcePlay(targetSrc);
        }
    }

    bool SoundGenerator::sourcePlaying(ALuint source)
    {
        ALint state;

        alGetSourcei(source, AL_SOURCE_STATE, &state);

        if(state == AL_PLAYING)
        {
            return true;
        }
        return false;
    }

    /* Use short since it stays constant at 16bit regardless of CPU (unlike int) */
    short* SoundGenerator::generateSoundWave(size_t bufferSize, jfloat pitch, short lastVal, bool onUpSwing)
    {
        // Construct sound buffer
        short *samples = (short*)malloc(bufferSize * sizeof(short));
        memset(samples, 0, bufferSize);

        float phi = (2.f * float(M_PI) * pitch) / SAMPLE_RATE;

        /* Calculate phase shift to make the sines of different buffers align */
        float phase = asin(lastVal / 32760.f);

        for(int i = 0; i < bufferSize; i ++)
        {
            if(onUpSwing)
            {
                samples[i] = 32760 * sin(phi * i + phase);
            }
            else
            {
                samples[i] = 32760 * sin(phi * i - phase + M_PI);
            }

            if(i > bufferSize - 5 || i < 5)
            {
                __android_log_print(ANDROID_LOG_INFO, SOUNDLOG, "%d %f", samples[i], phase);
            }
        }

        return samples;
    }
}