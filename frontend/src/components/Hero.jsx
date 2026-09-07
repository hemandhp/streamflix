import { Play, Shuffle } from 'lucide-react'

export default function Hero({ channel, onPlay, onShuffle }) {
  if (!channel) {
    return (
      <div className="relative flex h-[52vh] min-h-[360px] items-center justify-center border-b border-hairline bg-surface">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-hairline border-t-signal" />
      </div>
    )
  }

  return (
    <div className="relative flex h-[62vh] min-h-[430px] flex-col justify-end overflow-hidden bg-[#181818] px-6 pb-12 pt-20 md:px-10">
      <div
        aria-hidden
        className="absolute inset-0 opacity-[0.12]"
        style={{
          backgroundImage: channel.logo ? `url(${channel.logo})` : 'none',
          backgroundSize: '420px',
          backgroundPosition: 'center 30%',
          backgroundRepeat: 'no-repeat',
          filter: 'blur(6px)'
        }}
      />
      <div className="absolute inset-0 bg-gradient-to-t from-[#141414] via-[#141414]/75 to-transparent" />

      <div className="relative flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-red-500">
        <span className="live-dot h-2 w-2 rounded-full bg-red-600" />
        ON AIR — {String(1).padStart(3, '0')}
      </div>

      <h1 className="relative mt-3 max-w-2xl text-4xl font-bold leading-tight text-white md:text-6xl">
        {channel.name}
      </h1>

      <p className="relative mt-3 max-w-lg text-sm text-gray-300 md:text-base">
        {[channel.groupTitle, channel.country, channel.language].filter(Boolean).join(' · ')}
      </p>

      <div className="relative mt-6 flex flex-wrap items-center gap-3">
        <button
          onClick={() => onPlay(channel)}
          className="flex items-center gap-2 rounded-md bg-white px-6 py-3 font-semibold text-black transition hover:bg-gray-200"
        >
          <Play className="h-5 w-5 fill-void" strokeWidth={1} />
          Play channel
        </button>
        <button
          onClick={onShuffle}
          className="flex items-center gap-2 rounded-md bg-white/15 px-5 py-3 font-semibold text-white transition hover:bg-white/25"
        >
          <Shuffle className="h-4 w-4" />
          Another channel
        </button>
      </div>
    </div>
  )
}
