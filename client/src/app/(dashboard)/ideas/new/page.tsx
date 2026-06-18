"use client";

import { useRouter } from "next/navigation";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Card, CardContent } from "@/components/ui/card";
import { ideaService } from "@/services/idea.service";
import { categoryService } from "@/services/category.service";
import { useRequireAuth } from "@/hooks/use-auth";

const schema = z
  .object({
    title: z.string().min(3, "Title is required").max(200),
    description: z.string().min(20, "Description must be at least 20 characters"),
    category: z.string().min(1, "Category is required"),
    basePrice: z.number({ error: "Minimum budget is required" }).min(0.01, "Minimum budget must be greater than 0"),
    maxBudget: z.number().optional(),
    tags: z.string().optional(),
  })
  .refine((d) => !d.maxBudget || d.maxBudget >= d.basePrice, {
    message: "Max budget must be greater than min budget",
    path: ["maxBudget"],
  });

type FormData = z.infer<typeof schema>;

export default function NewIdeaPage() {
  useRequireAuth(["CREATOR"]);
  const router = useRouter();

  const { data: categories } = useQuery({
    queryKey: ["categories"],
    queryFn: () => categoryService.getAll(),
  });

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const mutation = useMutation({
    mutationFn: (data: FormData) =>
      ideaService.create({
        title: data.title,
        description: data.description,
        category: data.category,
        basePrice: data.basePrice,
        maxBudget: data.maxBudget,
        tags: data.tags ? data.tags.split(",").map((t) => t.trim()).filter(Boolean) : undefined,
      }),
    onSuccess: (idea) => {
      toast.success("Idea created successfully!");
      router.push(`/ideas/${idea.id}`);
    },
    onError: () => toast.error("Failed to create idea"),
  });

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader title="Submit New Idea" description="Share your vision with potential investors." />

      <Card>
        <CardContent className="pt-6">
          <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="space-y-5">
            <div>
              <Label htmlFor="title">Title</Label>
              <Input id="title" placeholder="AI-powered logistics platform" className="mt-1.5" {...register("title")} />
              {errors.title && <p className="mt-1 text-xs text-red-400">{errors.title.message}</p>}
            </div>

            <div>
              <Label htmlFor="description">Description</Label>
              <Textarea id="description" placeholder="Describe your idea, market opportunity, and vision..." className="mt-1.5" {...register("description")} />
              {errors.description && <p className="mt-1 text-xs text-red-400">{errors.description.message}</p>}
            </div>

            <div>
              <Label>Category</Label>
              <Controller
                name="category"
                control={control}
                render={({ field }) => (
                  <Select onValueChange={field.onChange} value={field.value}>
                    <SelectTrigger className="mt-1.5">
                      <SelectValue placeholder="Select category" />
                    </SelectTrigger>
                    <SelectContent>
                      {categories?.map((c) => (
                        <SelectItem key={c.id} value={c.slug}>{c.name}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
              {errors.category && <p className="mt-1 text-xs text-red-400">{errors.category.message}</p>}
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <Label htmlFor="basePrice">Min Budget ($)</Label>
                <Input id="basePrice" type="number" step="0.01" className="mt-1.5" {...register("basePrice", { valueAsNumber: true })} />
                {errors.basePrice && <p className="mt-1 text-xs text-red-400">{errors.basePrice.message}</p>}
              </div>
              <div>
                <Label htmlFor="maxBudget">Max Budget ($)</Label>
                <Input id="maxBudget" type="number" step="0.01" className="mt-1.5" {...register("maxBudget", { valueAsNumber: true })} />
                {errors.maxBudget && <p className="mt-1 text-xs text-red-400">{errors.maxBudget.message}</p>}
              </div>
            </div>

            <div>
              <Label htmlFor="tags">Tags (comma-separated)</Label>
              <Input id="tags" placeholder="ai, fintech, saas" className="mt-1.5" {...register("tags")} />
            </div>

            <div className="flex gap-3 pt-2">
              <Button type="submit" disabled={mutation.isPending}>
                {mutation.isPending ? "Submitting..." : "Submit Idea"}
              </Button>
              <Button type="button" variant="secondary" onClick={() => router.back()}>
                Cancel
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
