import { useEffect, useMemo, useState } from 'react'
import { getAllChannels, getCatalogStatus, getCategories } from './api/channelApi'
import { useDebouncedValue } from './hooks/useDebouncedValue'
import Navbar from './components/Navbar'
import Hero from './components/Hero'
import ChannelRow from './components/ChannelRow'
import SearchResultsGrid from './components/SearchResultsGrid'
import VideoPlayer from './components/VideoPlayer'

export default function App() {
  const [categories, setCategories] = useState([])
  const [sampleChannels, setSampleChannels] = useState([])
  const [heroIndex, setHeroIndex] = useState(0)
  const [selectedChannel, setSelectedChannel] = useState(null)
  const [query, setQuery] = useState('')
  const [loadState, setLoadState] = useState('loading') // 'loading' | 'catalog-warming' | 'ready' | 'error'

  const debouncedQuery = useDebouncedValue(query, 350)

  async function loadCatalog() {
    const [cats, sample] = await Promise.all([getCategories(), getAllChannels({ page: 0, size: 60 })])
    setCategories(cats)
    setSampleChannels(sample.items)
    if (sample.items.length > 0) {
      setHeroIndex(Math.floor(Math.random() * sample.items.length))
    }
    return sample.items.length > 0
  }

  useEffect(() => {
    let cancelled = false
    let pollTimer = null

    async function bootstrap() {
      try {
        const hasChannels = await loadCatalog()
        if (cancelled) return

        if (hasChannels) {
          setLoadState('ready')
          return
        }

        // Backend is reachable but hasn't finished its first playlist fetch/parse yet
        // (can take up to ~30s for the full ~8,000-channel index). Poll until it's ready.
        setLoadState('catalog-warming')
        pollTimer = setInterval(async () => {
          try {
            const status = await getCatalogStatus()
            if (cancelled) return
            if (status.loaded) {
              clearInterval(pollTimer)
              const ok = await loadCatalog()
              if (!cancelled) setLoadState(ok ? 'ready' : 'error')
            }
          } catch {
            // Transient - keep polling, the top-level request already proved the backend is up.
          }
        }, 3000)
      } catch {
        if (!cancelled) setLoadState('error')
      }
    }

    bootstrap()
    return () => {
      cancelled = true
      if (pollTimer) clearInterval(pollTimer)
    }
  }, [])

  const heroChannel = useMemo(
    () => (sampleChannels.length ? sampleChannels[heroIndex % sampleChannels.length] : null),
    [sampleChannels, heroIndex]
  )

  function shuffleHero() {
    if (sampleChannels.length < 2) return
    let next = Math.floor(Math.random() * sampleChannels.length)
    if (next === heroIndex % sampleChannels.length) next = (next + 1) % sampleChannels.length
    setHeroIndex(next)
  }

  if (loadState === 'error') {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-3 bg-[#141414] px-6 text-center text-white">
        <p className="font-display text-2xl font-semibold">Can't reach the StreamFlix API</p>
        <p className="max-w-md text-sm text-gray-400">
          Make sure the Spring Boot backend is running on <code className="text-white">http://localhost:8080</code>{' '}
          — see the project README for setup instructions.
        </p>
      </div>
    )
  }

  if (loadState === 'loading' || loadState === 'catalog-warming') {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-3 bg-[#141414] px-6 text-center text-white">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-gray-700 border-t-white" />
        <p className="mt-2 font-display text-xl font-semibold">
          {loadState === 'catalog-warming' ? 'Tuning in the channel guide…' : 'Connecting…'}
        </p>
        {loadState === 'catalog-warming' && (
          <p className="max-w-sm text-sm text-gray-400">
            The backend is downloading and parsing the full channel index for the first time —
            this can take up to 30 seconds. This page will update automatically.
          </p>
        )}
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-[#141414]">
      <Navbar query={query} onQueryChange={setQuery} />

      {debouncedQuery.trim() ? (
        <SearchResultsGrid query={debouncedQuery.trim()} onSelect={setSelectedChannel} />
      ) : (
        <>
          <Hero channel={heroChannel} onPlay={setSelectedChannel} onShuffle={shuffleHero} />
          <main className="flex flex-col gap-10 py-10">
            {categories.map((category) => (
              <ChannelRow key={category.name} category={category} onSelect={setSelectedChannel} />
            ))}
          </main>
        </>
      )}

      <VideoPlayer channel={selectedChannel} onClose={() => setSelectedChannel(null)} />
    </div>
  )
}
