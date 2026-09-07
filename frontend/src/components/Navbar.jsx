import { Search, X } from 'lucide-react'

export default function Navbar({ query, onQueryChange }) {
  return (
    <header className="fixed inset-x-0 top-0 z-30 flex items-center gap-5 bg-[#141414]/95 px-4 py-3 backdrop-blur-md md:px-8">
      <div className="flex items-baseline gap-2.5">
        <span className="text-2xl font-bold tracking-tight text-red-600">StreamFlix</span>
        <span className="hidden text-xs text-gray-400 sm:inline">live channel guide</span>
      </div>

      <div className="relative ml-auto w-full max-w-md">
        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
        <input
          value={query}
          onChange={(e) => onQueryChange(e.target.value)}
          type="text"
          placeholder="Search channels, countries, categories…"
          className="w-full rounded-md border border-gray-700 bg-[#222] py-2.5 pl-10 pr-9 text-sm text-white placeholder:text-gray-500 outline-none transition focus:border-gray-400"
        />
        {query && (
          <button
            onClick={() => onQueryChange('')}
            aria-label="Clear search"
            className="absolute right-2.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white"
          >
            <X className="h-4 w-4" />
          </button>
        )}
      </div>
    </header>
  )
}
