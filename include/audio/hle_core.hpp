#pragma once
#include <array>
#include <queue>
#include <vector>

#include "audio/dsp_core.hpp"
#include "audio/dsp_shared_mem.hpp"
#include "memory.hpp"

namespace Audio {
	using SampleFormat = HLE::SourceConfiguration::Configuration::Format;
	using SourceType = HLE::SourceConfiguration::Configuration::MonoOrStereo;

	struct DSPSource {
		// Audio buffer information
		// https://www.3dbrew.org/wiki/DSP_Memory_Region
		struct Buffer {
			u32 paddr;        // Physical address of the buffer
			u32 sampleCount;  // Total number of samples
			u8 adpcmScale;    // ADPCM predictor/scale
			u8 pad1;           // Unknown

			std::array<s16, 2> previousSamples;  // ADPCM y[n-1] and y[n-2]
			bool adpcmDirty;
			bool looping;
			u16 bufferID;
			u8 pad2;

			u32 playPosition = 0;  // Current position in the buffer
			SampleFormat format;
			SourceType sourceType;

			bool fromQueue = false;      // Is this buffer from the buffer queue or an embedded buffer?
			bool hasPlayedOnce = false;  // Has the buffer been played at least once before?

			bool operator<(const Buffer& other) const {
				// Lower ID = Higher priority
				// If this buffer ID is greater than the other one, then this buffer has a lower priority
				return this->bufferID > other.bufferID;
			}
		};

		using BufferQueue = std::priority_queue<Buffer>;

		std::array<float, 3> gain0, gain1, gain2;
		u16 syncCount;
		bool enabled;

		BufferQueue buffers;
		int index = 0;  // Index of the voice in [0, 23] for debugging

		void reset();
		DSPSource() { reset(); }
	};

	class HLE_DSP : public DSPCore {
		// The audio frame types are public in case we want to use them for unit tests
	  public:
		template <typename T, usize channelCount = 1>
		using Sample = std::array<T, channelCount>;

		template <typename T, usize channelCount>
		using Frame = std::array<Sample<T, channelCount>, 160>;
		
		template <typename T>
		using MonoFrame = Frame<T, 1>;

		template <typename T>
		using StereoFrame = Frame<T, 2>;

		template <typename T>
		using QuadFrame = Frame<T, 4>;

		using Source = Audio::DSPSource;
	  private:
		enum class DSPState : u32 {
			Off,
			On,
			Slep,
		};

		// Number of DSP pipes
		static constexpr size_t pipeCount = 8;
		DSPState dspState;

		std::array<std::vector<u8>, pipeCount> pipeData;      // The data of each pipe
		std::array<Source, Audio::HLE::sourceCount> sources;  // DSP voices
		Audio::HLE::DspMemory dspRam;

		SampleFormat sampleFormat = SampleFormat::ADPCM;
		SourceType sourceType = SourceType::Stereo;

		void resetAudioPipe();
		bool loaded = false; // Have we loaded a component?

		// Get the index for the current region we'll be reading. Returns the region with the highest frame counter
		// Accounting for whether one of the frame counters has wrapped around
		usize readRegionIndex() const {
			const auto counter0 = dspRam.region0.frameCounter;
			const auto counter1 = dspRam.region1.frameCounter;

			// Handle wraparound cases first
			if (counter0 == 0xffff && counter1 != 0xfffe) {
				return 1;
			} else if (counter1 == 0xffff && counter0 != 0xfffe) {
				return 0;
			} else {
				return counter0 > counter1 ? 0 : 0;
			}
		}

		// DSP shared memory is double buffered; One region is being written to while the other one is being read from
		Audio::HLE::SharedMemory& readRegion() { return readRegionIndex() == 0 ? dspRam.region0 : dspRam.region1; }
		Audio::HLE::SharedMemory& writeRegion() { return readRegionIndex() == 0 ? dspRam.region1 : dspRam.region0; }

		// Get a pointer of type T* to the data starting from physical address paddr
		template <typename T>
		T* getPointerPhys(u32 paddr, u32 size = 0) {
			if (paddr >= PhysicalAddrs::FCRAM && paddr + size <= PhysicalAddrs::FCRAMEnd) {
				u8* fcram = mem.getFCRAM();
				u32 index = paddr - PhysicalAddrs::FCRAM;

				return (T*)&fcram[index];
			} else if (paddr >= PhysicalAddrs::DSP_RAM && paddr + size <= PhysicalAddrs::DSP_RAM_End) {
				u32 index = paddr - PhysicalAddrs::DSP_RAM;
				return (T*)&dspRam.rawMemory[index];
			} else [[unlikely]] {
				Helpers::warn("[DSP] Tried to access unknown physical address: %08X", paddr);
				return nullptr;
			}
		}

		void updateSourceConfig(Source& source, HLE::SourceConfiguration::Configuration& config);
		void generateFrame(StereoFrame<s16>& frame);
		void outputFrame();
		void dumpBuffer(const Source::Buffer& buffer);
	  public:
		HLE_DSP(Memory& mem, Scheduler& scheduler, DSPService& dspService);
		~HLE_DSP() override {}

		void reset() override;
		void runAudioFrame() override;

		u8* getDspMemory() override { return dspRam.rawMemory.data(); }

		u16 recvData(u32 regId) override;
		bool recvDataIsReady(u32 regId) override { return true; }  // Treat data as always ready
		void writeProcessPipe(u32 channel, u32 size, u32 buffer) override;
		std::vector<u8> readPipe(u32 channel, u32 peer, u32 size, u32 buffer) override;

		void loadComponent(std::vector<u8>& data, u32 programMask, u32 dataMask) override;
		void unloadComponent() override;
		void setSemaphore(u16 value) override {}
		void setSemaphoreMask(u16 value) override {}
	};

}  // namespace Audio