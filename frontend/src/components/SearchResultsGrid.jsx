import { useEffect, useState } from 'react'
import { searchChannels } from '../api/channelApi'
import ChannelCard from './ChannelCard'

export default function SearchResultsGrid({ query, onSelect }) {
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [items, setItems] = useState([])

  useEffect(() => {
    setPage(0)
    setItems([])
  }, [query])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    searchChannels(query, { page, size: 40 })
      .then((res) => {
        if (cancelled) return
        setResult(res)
        setItems((prev) => (page === 0 ? res.items : [...prev, ...res.items]))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [query, page])

  const hasMore = result && page + 1 < result.totalPages

  return (
    <section className="px-6 pb-16 pt-28 md:px-10">
      <h2 className="mb-1 font-display text-2xl font-semibold text-white">
        Results for “{query}”
      </h2>
      <p className="mb-6 font-mono text-xs text-gray-400">
        {result ? `${result.totalItems} channel${result.totalItems === 1 ? '' : 's'}` : '…'}
      </p>

      {items.length === 0 && !loading ? (
        <p className="text-gray-400">No channels matched that search. Try a country, language, or category instead.</p>
      ) : (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-5 lg:grid-cols-6">
          {items.map((channel, i) => (
            <ChannelCard key={channel.id} channel={channel} index={i} onSelect={onSelect} />
          ))}
        </div>
      )}

      {loading && (
        <div className="mt-6 flex justify-center">
          <div className="h-6 w-6 animate-spin rounded-full border-2 border-hairline border-t-signal" />
        </div>
      )}

      {hasMore && !loading && (
        <div className="mt-8 flex justify-center">
          <button
            onClick={() => setPage((p) => p + 1)}
            className="rounded-sm border border-hairline px-5 py-2 text-sm text-white hover:border-signal"
          >
            Load more
          </button>
        </div>
      )}
    </section>
  )
}
