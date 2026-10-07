"use client";

import { HeroUIProvider } from "@heroui/react";
import { LazyMotion, domAnimation } from "framer-motion";

export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <HeroUIProvider>
      <LazyMotion features={domAnimation}>{children}</LazyMotion>
    </HeroUIProvider>
  );
}
