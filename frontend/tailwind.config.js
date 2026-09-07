/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        void: '#0A0D13',
        surface: '#12161F',
        raised: '#1A2030',
        hairline: '#262C3C',
        ink: '#E9EBF0',
        muted: '#8B93A7',
        signal: '#FF5A36',
        'signal-dim': '#7A3222',
        phosphor: '#37D67A'
      },
      fontFamily: {
        display: ['"Big Shoulders Display"', 'sans-serif'],
        sans: ['"IBM Plex Sans"', 'sans-serif'],
        mono: ['"IBM Plex Mono"', 'monospace']
      }
    }
  },
  plugins: []
}
