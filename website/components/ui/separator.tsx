"use client";

import * as React from "react";
import { cn } from "@/lib/utils";
import { cva, type VariantProps } from "class-variance-authority";
import { Slot } from "@radix-ui/react-slot";

interface SeparatorProps
  extends React.HTMLAttributes<HTMLHRElement>,
    VariantProps<typeof separatorVariants> {
  decorative?: boolean;
}

const Separator = React.forwardRef<HTMLHRElement, SeparatorProps>(
  ({ className, variant, orientation = "horizontal", decorative = false, ...props }, ref) => (
    <hr
      ref={ref}
      className={cn(
        separatorVariants({ variant, orientation, className }),
        "border-rule shrink-0 [&>hr]:shrink-0 [&>hr]:border-current [&>hr]:h-full [&>hr]:w-full",
      )}
      aria-orientation={orientation as "horizontal" | "vertical" | undefined}
      {...(decorative ? { "aria-hidden": true } : {})}
      {...props}
    />
  ),
);
Separator.displayName = "Separator";

const separatorVariants = cva("relative flex w-full shrink-0 border-t", {
  variants: {
    orientation: {
      horizontal: "h-[1px] w-full",
      vertical: "h-full w-[1px] border-l",
    },
    variant: {
      default: "",
      dashed: "border-dashed",
      dotted: "border-dotted",
    },
  },
  defaultVariants: {
    orientation: "horizontal",
    variant: "default",
  },
});

export { Separator, separatorVariants };
