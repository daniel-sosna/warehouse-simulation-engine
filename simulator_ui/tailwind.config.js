/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: "#09111f",
        panel: "#111b2e",
        border: "#26324b",
        accent: "#6ee7b7",
        warn: "#f59e0b",
        danger: "#fb7185"
      },
      boxShadow: {
        panel: "0 18px 48px rgba(5, 15, 35, 0.24)"
      }
    },
  },
  plugins: [],
};
