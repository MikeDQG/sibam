import { Route, Routes } from "react-router-dom";
import { Login } from "./components/Authorization/Login";
import { Register } from "./components/Authorization/Register";
import { LandingPage } from "./components/Pages/LandingPage";
import { MainAppHome } from "./components/Pages/MainAppHome";
import { AccountPage } from "./components/Pages/AccountPage";
function App() {
    return (
        <Routes>
            <Route path="/" element={<LandingPage />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/home" element={<MainAppHome />} />
            <Route path="/account" element={<AccountPage />} />
        </Routes>
    );
}

export default App;
