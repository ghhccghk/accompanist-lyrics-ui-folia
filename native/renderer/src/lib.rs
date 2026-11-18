// SPDX-License-Identifier: AGPL-3.0-only
//! Optional Android renderer. No playback, clock, network or application bridge.
pub const ABI_VERSION: i32 = 2;
pub const MAX_DIMENSION: u32 = 8192;
pub mod protocol;
#[cfg(target_os = "android")]
mod gpu;

pub fn dimensions(width: i32, height: i32) -> Result<(u32, u32), i32> {
    if width <= 0 || height <= 0 || width as u32 > MAX_DIMENSION || height as u32 > MAX_DIMENSION {
        Err(-2)
    } else {
        Ok((width as u32, height as u32))
    }
}

/// IDs are monotonic, never pointer casts or recycled indices. Stale/double
/// destroys are harmless; exhaustion fails rather than reusing an old handle.
#[derive(Default)]
pub struct Sessions<T> {
    next: i64,
    values: std::collections::BTreeMap<i64, T>,
}
impl<T> Sessions<T> {
    pub fn insert(&mut self, value: T) -> Result<i64, i32> {
        self.next = self.next.checked_add(1).ok_or(-3)?;
        self.values.insert(self.next, value);
        Ok(self.next)
    }
    pub fn get_mut(&mut self, id: i64) -> Result<&mut T, i32> { self.values.get_mut(&id).ok_or(-4) }
    pub fn remove(&mut self, id: i64) { self.values.remove(&id); }
}

#[cfg(target_os = "android")]
mod android;

#[cfg(test)]
mod tests {
    use super::*;
    #[test] fn bounded_dimensions() {
        for (w,h) in [(0,1),(-1,1),(1,0),(8193,1),(i32::MAX,i32::MAX)] { assert!(dimensions(w,h).is_err()); }
        assert_eq!(dimensions(800,600),Ok((800,600)));
    }
    #[test] fn independent_handles_never_recycle() {
        let mut sessions = Sessions::default();
        let a = sessions.insert(10).unwrap();
        let b = sessions.insert(20).unwrap();
        sessions.remove(a); sessions.remove(a);
        assert!(sessions.get_mut(a).is_err());
        assert_eq!(*sessions.get_mut(b).unwrap(),20);
        assert!(sessions.insert(30).unwrap()>b);
    }
}
