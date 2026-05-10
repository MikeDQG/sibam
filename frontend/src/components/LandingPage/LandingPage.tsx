import { FeaturesSection } from "./Parts/FeaturesSection";
import { Footer } from "./Parts/Footer";
import { Header } from "./Parts/Header";
import { HeroSection } from "./Parts/HeroSection";

export const LandingPage = () => {
  return (
    <>
      <Header />
      <HeroSection />
      <FeaturesSection />
      <Footer />
    </>
  );
};
