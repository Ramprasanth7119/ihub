import { Navbar } from "@/components/layout/navbar";
import { Footer } from "@/components/layout/footer";
import { Hero } from "@/features/landing/hero";
import { FeaturesSection } from "@/features/landing/features-section";
import { HowItWorks } from "@/features/landing/how-it-works";
import { Benefits } from "@/features/landing/benefits";
import { Testimonials } from "@/features/landing/testimonials";
import { CTA } from "@/features/landing/cta";

export default function LandingPage() {
  return (
    <>
      <Navbar />
      <main>
        <Hero />
        <FeaturesSection />
        <HowItWorks />
        <Benefits />
        <Testimonials />
        <CTA />
      </main>
      <Footer />
    </>
  );
}
