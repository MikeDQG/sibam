import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { onAuthStateChanged } from "firebase/auth";
import { auth } from "../../firebase";

const apiUrl = import.meta.env.VITE_API_URL;

export type UserSession = {
  id: string;
  name: string | null;
  email: string;
};

type BackendUser = {
  id: string;
  email?: string | null;
  fullName?: string | null;
};

type UserSessionContextValue = {
  userSession: UserSession | null;
  setUserSession: (session: UserSession | null) => void;
  clearUserSession: () => void;
  getAuthToken: () => Promise<string | null>;
  fetchUserSession: (token: string) => Promise<UserSession>;
  syncUserSession: (token: string, fullName?: string) => Promise<UserSession>;
};

const UserSessionContext = createContext<UserSessionContextValue | null>(null);

function normalizeUser(user: BackendUser): UserSession {
  return {
    id: user.id,
    name: user.fullName ?? null,
    email: user.email ?? "",
  };
}

type UserSessionProviderProps = {
  children: ReactNode;
};

export function UserSessionProvider({ children }: UserSessionProviderProps) {
  const [userSession, setUserSession] = useState<UserSession | null>(null);

  const clearUserSession = useCallback(() => {
    setUserSession(null);
  }, []);

  const getAuthToken = useCallback(async () => {
    const user = auth.currentUser;

    if (!user) return null;

    return user.getIdToken();
  }, []);

  const fetchUserSession = useCallback(
    async (token: string) => {
      const response = await fetch(`${apiUrl}/api/users/me`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error(`User session request failed: ${response.status}`);
      }

      const session = normalizeUser((await response.json()) as BackendUser);
      setUserSession(session);
      return session;
    },
    [setUserSession],
  );

  const syncUserSession = useCallback(
    async (token: string, fullName?: string) => {
      const headers: Record<string, string> = {
        Authorization: `Bearer ${token}`,
      };

      if (fullName?.trim()) {
        headers["X-Full-Name"] = fullName.trim();
      }

      const response = await fetch(`${apiUrl}/api/users/me`, {
        method: "POST",
        headers,
      });

      if (!response.ok) {
        throw new Error(`User session sync failed: ${response.status}`);
      }

      const session = normalizeUser((await response.json()) as BackendUser);
      setUserSession(session);
      return session;
    },
    [setUserSession],
  );

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (user) => {
      if (!user) {
        clearUserSession();
        return;
      }

      try {
        await fetchUserSession(await user.getIdToken());
      } catch {
        clearUserSession();
      }
    });

    return () => unsubscribe();
  }, [clearUserSession, fetchUserSession]);

  const value = useMemo(
    () => ({
      userSession,
      setUserSession,
      clearUserSession,
      getAuthToken,
      fetchUserSession,
      syncUserSession,
    }),
    [
      userSession,
      setUserSession,
      clearUserSession,
      getAuthToken,
      fetchUserSession,
      syncUserSession,
    ],
  );

  return (
    <UserSessionContext.Provider value={value}>
      {children}
    </UserSessionContext.Provider>
  );
}

export function useUserSession() {
  const context = useContext(UserSessionContext);

  if (!context) {
    throw new Error("useUserSession must be used within UserSessionProvider");
  }

  return context;
}
