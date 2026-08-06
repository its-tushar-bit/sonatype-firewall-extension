/** @type {import('tailwindcss').Config} */
export default {
  content: ["./src/**/*.{ts,tsx,html}"],
  theme: {
    extend: {
      colors: {
        sonatype: {
          blue: "#1F65BF",
          dark: "#0A2540",
          danger: "#D92D20",
          warn: "#F79009",
          ok: "#12B76A",
        },
      },
    },
  },
  plugins: [],
};
