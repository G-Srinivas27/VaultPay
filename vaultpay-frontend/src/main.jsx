// main.jsx
// Application entry point — mounts the React app into the DOM.
//
// BrowserRouter: Enables React Router's URL-based navigation.
// Must wrap the entire App so that useNavigate, Link,
// and Routes work anywhere in the component tree.
//
// NOTE: StrictMode is intentionally removed during development.
// It double-invokes useEffect hooks which causes every API call to fire twice,
// triggering our backend rate limiter (60 req/min). StrictMode does NOT exist
// in production builds — this is purely a dev environment adjustment.

import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { ToastProvider } from './contexts/ToastContext'
import './index.css'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <ToastProvider>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </ToastProvider>,
)
