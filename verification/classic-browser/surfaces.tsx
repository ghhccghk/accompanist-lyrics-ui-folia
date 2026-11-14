// Scope isolation only: original classic/GlowWord/timing/geometry are imported unchanged.
// Background engine, shell chrome and subtitle overlay are outside this text-only reference.
import React from 'react';
// Waiting UI is deliberately outside the text comparison, as are subtitles/chrome.
export const useTranslation = () => ({ t: (_key: string) => '' });
export default function Shell({ children }: any) {
  return <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center',
    background: '#162435', fontFamily: 'Parity Noto', color: '#dfe5ec' }}>{children}</div>;
}
