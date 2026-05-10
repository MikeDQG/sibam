import { Route, Routes } from "react-router-dom";
import { Login } from "./components/Authorization/Login";
import { Register } from "./components/Authorization/Register";
import { LandingPage } from "./components/Pages/LandingPage";
import { MainAppHome } from "./components/Pages/MainAppHome";
function App() {
  return (
    <Routes>
      <Route path='/' element={<LandingPage />} />
      <Route path='/login' element={<Login />} />
      <Route path='/register' element={<Register />} />
      <Route path='/home' element={<MainAppHome />} />
    </Routes>
  );
}

export default App;
