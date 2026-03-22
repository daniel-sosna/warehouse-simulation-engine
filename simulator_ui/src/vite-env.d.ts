/// <reference types="vite/client" />

declare module "*.log?raw" {
  const content: string;
  export default content;
}
