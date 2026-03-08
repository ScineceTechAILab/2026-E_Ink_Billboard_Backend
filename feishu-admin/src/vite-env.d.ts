
/// &lt;reference types="vite/client" /&gt;

interface ImportMetaEnv {
  readonly VITE_API_URL: string
  readonly VITE_FEISHU_APP_ID: string
  readonly VITE_FEISHU_REDIRECT_URI: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

