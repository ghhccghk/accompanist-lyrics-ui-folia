// Host setting boundary only; the original quality resolver and Cadenza drawing
// remain untouched. Avoid importing the application's entire settings registry.
const state = Object.freeze({lyricRenderQuality:Object.freeze({scale:1,antialias:'auto'})});
export const useVisualizerSettingsStore = <T>(selector:(value:typeof state)=>T):T => selector(state);
