import { Route, Routes } from "react-router-dom";
import { Login } from "./components/Authorization/Login";
import { Register } from "./components/Authorization/Register";
import { LandingPage } from "./components/LandingPage/LandingPage";
function App() {
  return (
    <Routes>
      <Route path='/' element={<LandingPage />} />
      <Route path='/login' element={<Login />} />
      <Route path='/register' element={<Register />} />
    </Routes>
  );
}

export default App;
