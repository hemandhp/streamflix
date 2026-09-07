import { Play } from 'lucide-react'

function channelNumber(index) {
  return `CH ${String(index + 1).padStart(3, '0')}`
}

export default function ChannelCard({ channel, index, onSelect }) {
  return (
    <button
      onClick={() => onSelect(channel)}
      className="group relative w-44 shrink-0 text-left focus:outline-none sm:w-48"
      aria-label={`Play ${channel.name}`}
    >
      <div className="relative aspect-video overflow-hidden rounded-md bg-[#222] transition duration-200 group-hover:-translate-y-1 group-hover:shadow-xl group-focus-visible:ring-2 group-focus-visible:ring-white">
        {channel.logo ? (
          <img
            src={channel.logo}
            alt=""
            loading="lazy"
            className="h-full w-full object-contain p-5 transition duration-200 group-hover:scale-105"
            onError={(e) => {
              e.currentTarget.style.display = 'none'
            }}
          />
        ) : null}

        <div className="pointer-events-none absolute left-2 top-2 rounded bg-black/70 px-2 py-1 text-[10px] font-medium tracking-wide text-gray-300">
          {channelNumber(index)}
        </div>

        <div className="absolute inset-0 flex items-center justify-center bg-black/50 opacity-0 transition-opacity group-hover:opacity-100">
          <Play className="h-10 w-10 fill-white text-white" strokeWidth={1} />
        </div>
      </div>

      <div className="mt-2 flex items-start justify-between gap-1">
        <p className="line-clamp-2 text-sm font-medium leading-tight text-white">{channel.name}</p>
      </div>
      <div className="mt-1 flex items-center gap-1 text-xs text-gray-400">
        <span className="live-dot h-1.5 w-1.5 rounded-full bg-red-600" />
        <span>{channel.country || channel.language || channel.groupTitle}</span>
      </div>
    </button>
  )
}
