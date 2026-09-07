import { useEffect, useRef, useState } from 'react'
import Hls from 'hls.js'
import { X, TriangleAlert } from 'lucide-react'
import { proxiedStreamUrl } from '../api/channelApi'

export default function VideoPlayer({ channel, onClose }) {
  const videoRef = useRef(null)
  const hlsRef = useRef(null)
  const [status, setStatus] = useState('loading') // 'loading' | 'ready' | 'error'
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    if (!channel) return

    const video = videoRef.current
    const src = proxiedStreamUrl(channel.streamUrl)
    setStatus('loading')
    setErrorMessage('')

    function fail(message) {
      setStatus('error')
      setErrorMessage(message)
    }

    if (Hls.isSupported()) {
      const hls = new Hls({ maxBufferLength: 30 })
      hlsRef.current = hls
      hls.loadSource(src)
      hls.attachMedia(video)
      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        setStatus('ready')
        video.play().catch(() => {
          /* Autoplay with sound can be blocked - the native controls let the user press play. */
        })
      })
      hls.on(Hls.Events.ERROR, (_event, data) => {
        if (data.fatal) {
          fail('This channel could not be loaded. It may be offline, geo-restricted, or the source has moved.')
        }
      })
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
      video.src = src
      video.addEventListener('loadedmetadata', () => setStatus('ready'), { once: true })
      video.addEventListener('error', () => fail('This channel could not be loaded.'), { once: true })
    } else {
      fail('Your browser does not support HLS live streaming.')
    }

    return () => {
      hlsRef.current?.destroy()
      hlsRef.current = null
      if (video) {
        video.removeAttribute('src')
        video.load()
      }
    }
  }, [channel])

  useEffect(() => {
    function handleKey(e) {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', handleKey)
    return () => window.removeEventListener('keydown', handleKey)
  }, [onClose])

  if (!channel) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/90 p-3 md:p-8"
      onClick={onClose}
    >
      <div
        className="relative flex w-full max-w-6xl flex-col overflow-hidden rounded-lg bg-[#181818] shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between border-b border-gray-800 px-4 py-3">
          <div className="flex items-center gap-2 overflow-hidden">
            {channel.logo && <img src={channel.logo} alt="" className="h-7 w-7 shrink-0 rounded object-contain" />}
            <div className="truncate">
              <p className="truncate text-base font-semibold leading-none text-white">{channel.name}</p>
              <p className="truncate text-xs text-gray-400">
                {[channel.groupTitle, channel.country].filter(Boolean).join(' · ')}
              </p>
            </div>
          </div>
          <button onClick={onClose} aria-label="Close player" className="shrink-0 rounded p-1 text-gray-400 transition hover:bg-white/10 hover:text-white">
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="relative aspect-video bg-black">
          <video ref={videoRef} controls playsInline className="h-full w-full" />

          {status === 'loading' && (
            <div className="pointer-events-none absolute inset-0 flex items-center justify-center">
              <div className="h-8 w-8 animate-spin rounded-full border-2 border-gray-700 border-t-white" />
            </div>
          )}

          {status === 'error' && (
            <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 bg-[#141414] px-6 text-center">
              <TriangleAlert className="h-8 w-8 text-red-500" strokeWidth={1.5} />
              <p className="max-w-sm text-sm text-gray-400">{errorMessage}</p>
              <p className="max-w-sm text-xs text-gray-400">
                Public IPTV streams change often — try another channel from the guide.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
