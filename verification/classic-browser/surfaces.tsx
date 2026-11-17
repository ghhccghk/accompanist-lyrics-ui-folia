// Scope isolation only: original classic/GlowWord/timing/geometry are imported unchanged.
// Background engine, shell chrome and subtitle overlay are outside this text-only reference.
import React from 'react';
// Waiting UI is deliberately outside the text comparison, as are subtitles/chrome.
export const useTranslation = () => ({ t: (_key: string) => '' });
export default function Shell({ children, theme, ref }: any) {
  // React 19 forwards ref as a prop. Cadenza observes the real shell dimensions;
  // dropping it leaves its viewport at zero and never starts the source stage.
  return <div ref={ref} style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center',
    background: '#162435', fontFamily: theme.fontFamily, color: '#dfe5ec' }}>{children}</div>;
}
