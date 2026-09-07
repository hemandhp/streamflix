import { useEffect, useRef, useState } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { getChannelsByCategory } from '../api/channelApi'
import ChannelCard from './ChannelCard'

export default function ChannelRow({ category, onSelect }) {
  const [channels, setChannels] = useState([])
  const [loading, setLoading] = useState(true)
  const railRef = useRef(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    getChannelsByCategory(category.name, { page: 0, size: 20 })
      .then((res) => {
        if (!cancelled) setChannels(res.items)
      })
      .catch(() => {
        if (!cancelled) setChannels([])
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [category.name])

  function scrollBy(amount) {
    railRef.current?.scrollBy({ left: amount, behavior: 'smooth' })
  }

  if (!loading && channels.length === 0) return null

  return (
    <section className="group/row relative">
      <div className="mb-4 flex items-baseline justify-between px-4 md:px-8">
        <h2 className="text-xl font-semibold text-white">
          {category.name}
        </h2>
        <span className="text-xs text-gray-500">{category.channelCount} channels</span>
      </div>

      <div className="relative">
        <button
          onClick={() => scrollBy(-600)}
          aria-label="Scroll left"
          className="absolute left-0 top-0 z-10 hidden h-full w-10 items-center justify-center bg-gradient-to-r from-void to-transparent opacity-0 transition-opacity group-hover/row:opacity-100 md:flex"
        >
          <ChevronLeft className="h-6 w-6" />
        </button>

        <div ref={railRef} className="rail flex gap-4 overflow-x-auto scroll-smooth px-4 pb-2 md:px-8">
          {loading
            ? Array.from({ length: 8 }).map((_, i) => (
                <div key={i} className="aspect-video w-44 shrink-0 animate-pulse rounded-md bg-[#222] sm:w-48" />
              ))
            : channels.map((channel, i) => (
                <ChannelCard key={channel.id} channel={channel} index={i} onSelect={onSelect} />
              ))}
        </div>

        <button
          onClick={() => scrollBy(600)}
          aria-label="Scroll right"
          className="absolute right-0 top-0 z-10 hidden h-full w-10 items-center justify-center bg-gradient-to-l from-void to-transparent opacity-0 transition-opacity group-hover/row:opacity-100 md:flex"
        >
          <ChevronRight className="h-6 w-6" />
        </button>
      </div>
    </section>
  )
}
