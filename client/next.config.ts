import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8081"}/api/:path*`,
      },
      {
        source: "/ws/:path*",
        destination: `${process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8081"}/ws/:path*`,
      },
    ];
  },
};

export default nextConfig;
