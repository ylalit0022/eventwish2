import { initializeApp } from 'firebase/app';
import { 
  getAuth, 
  GoogleAuthProvider, 
  signInWithPopup,
  signInWithRedirect, 
  getRedirectResult, 
  signOut,
  onAuthStateChanged,
  setPersistence,
  browserLocalPersistence
} from 'firebase/auth';

// Firebase configuration with hardcoded values
const firebaseConfig = {
  apiKey: "AIzaSyDRXTiDWClDkwqWUjHyg49z0UW5R8p81qs",
  authDomain: "neweventwish.firebaseapp.com",
  databaseURL: "https://neweventwish-default-rtdb.firebaseio.com",
  projectId: "neweventwish",
  storageBucket: "neweventwish.firebasestorage.app",
  messagingSenderId: "381021411563",
  appId: "1:381021411563:android:3fae237c5ecc58610906ae"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const googleProvider = new GoogleAuthProvider();

// Set persistence to LOCAL (survives browser restarts)
setPersistence(auth, browserLocalPersistence)
  .then(() => {
    console.log("Firebase persistence set to LOCAL");
  })
  .catch((error) => {
    console.error("Error setting persistence:", error);
  });

// Use the web client ID for Google Sign-In
googleProvider.setCustomParameters({
  client_id: "381021411563-8db9brsoa88i8v2682no16uc392j5qhe.apps.googleusercontent.com",
  prompt: "select_account"
});

// Sign in with Google using redirect (more compatible with CSP restrictions)
export const signInWithGoogle = async () => {
  try {
    console.log("Starting Google sign in with redirect...");
    await signInWithRedirect(auth, googleProvider);
    // The result will be handled by getRedirectResult in AuthContext
    return true;
  } catch (error) {
    console.error("Error initiating Google sign in redirect:", error);
    throw error;
  }
};

// Get result from redirect
export const getGoogleRedirectResult = async () => {
  try {
    console.log("Getting redirect result...");
    const result = await getRedirectResult(auth);
    if (result) {
      console.log("Got redirect result:", result.user.email);
      return result;
    }
    console.log("No redirect result found");
    return null;
  } catch (error) {
    console.error("Error getting redirect result:", error);
    throw error;
  }
};

// Sign out
export const logOut = async () => {
  try {
    await signOut(auth);
    console.log("Signed out successfully");
  } catch (error) {
    console.error("Error signing out:", error);
    throw error;
  }
};

// Get current user
export const getCurrentUser = () => {
  return new Promise((resolve, reject) => {
    const unsubscribe = onAuthStateChanged(auth, user => {
      unsubscribe();
      if (user) {
        console.log("Current user:", user.email);
      } else {
        console.log("No current user");
      }
      resolve(user);
    }, reject);
  });
};

// Get auth token
export const getAuthToken = async () => {
  const user = auth.currentUser;
  if (user) {
    try {
      const token = await user.getIdToken(true); // Force refresh
      console.log("Got auth token for user:", user.email);
      return token;
    } catch (error) {
      console.error("Error getting auth token:", error);
      return null;
    }
  }
  console.log("No current user, can't get token");
  return null;
};

export { auth };
export default app; 