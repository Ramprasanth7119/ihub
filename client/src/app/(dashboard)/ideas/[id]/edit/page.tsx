"use client";

import { use, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import { PageHeader } from "@/components/shared/page-header";
import { ErrorState } from "@/components/shared/error-state";
import { Skeleton } from "@/components/ui/skeleton";
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
import { useAuthStore } from "@/store/auth-store";

const schema = z.object({
  title: z.string().min(3).max(200),
  description: z.string().min(20),
  category: z.string().min(1),
  basePrice: z.number().min(0.01),
  maxBudget: z.number().optional(),
  tags: z.string().optional(),
});

type FormData = z.infer<typeof schema>;

export default function EditIdeaPage({ params }: { params: Promise<{ id: string }> }) {
  useRequireAuth(["CREATOR"]);
  const { id } = use(params);
  const ideaId = Number(id);
  const router = useRouter();
  const userId = useAuthStore((s) => s.user?.id);

  const { data: idea, isLoading, isError, refetch } = useQuery({
    queryKey: ["ideas", ideaId],
    queryFn: () => ideaService.getById(ideaId),
  });

  const { data: categories } = useQuery({
    queryKey: ["categories"],
    queryFn: () => categoryService.getAll(),
  });

  const { register, handleSubmit, control, reset, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      title: "",
      description: "",
      category: "",
      basePrice: 0,
      tags: "",
    },
  });

  useEffect(() => {
    if (idea) {
      reset({
        title: idea.title,
        description: idea.description,
        category: idea.category,
        basePrice: idea.basePrice,
        maxBudget: idea.maxBudget,
        tags: idea.tags?.join(", ") ?? "",
      });
    }
  }, [idea, reset]);

  const mutation = useMutation({
    mutationFn: (data: FormData) =>
      ideaService.update(ideaId, {
        title: data.title,
        description: data.description,
        category: data.category,
        basePrice: data.basePrice,
        maxBudget: data.maxBudget,
        tags: data.tags ? data.tags.split(",").map((t) => t.trim()).filter(Boolean) : undefined,
      }),
    onSuccess: () => {
      toast.success("Idea updated successfully!");
      router.push(`/ideas/${ideaId}`);
    },
    onError: () => toast.error("Failed to update idea. Only draft ideas can be edited."),
  });

  if (isLoading) return <Skeleton className="mx-auto h-96 max-w-2xl" />;
  if (isError || !idea) return <ErrorState onRetry={() => refetch()} />;

  if (idea.creatorId !== userId) {
    return <ErrorState message="You can only edit your own ideas." />;
  }

  if (idea.status !== "DRAFT") {
    return (
      <div className="mx-auto max-w-lg text-center">
        <ErrorState message="Only draft ideas can be edited." onRetry={() => router.push(`/ideas/${ideaId}`)} />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader title="Edit Idea" description="Update your draft before publishing." />

      <Card>
        <CardContent className="pt-6">
          <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="space-y-5">
            <div>
              <Label htmlFor="title">Title</Label>
              <Input id="title" className="mt-1.5" {...register("title")} />
              {errors.title && <p className="mt-1 text-xs text-red-400">{errors.title.message}</p>}
            </div>

            <div>
              <Label htmlFor="description">Description</Label>
              <Textarea id="description" className="mt-1.5" {...register("description")} />
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
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <Label htmlFor="basePrice">Min Budget ($)</Label>
                <Input id="basePrice" type="number" step="0.01" className="mt-1.5" {...register("basePrice", { valueAsNumber: true })} />
              </div>
              <div>
                <Label htmlFor="maxBudget">Max Budget ($)</Label>
                <Input id="maxBudget" type="number" step="0.01" className="mt-1.5" {...register("maxBudget", { valueAsNumber: true })} />
              </div>
            </div>

            <div>
              <Label htmlFor="tags">Tags (comma-separated)</Label>
              <Input id="tags" className="mt-1.5" {...register("tags")} />
            </div>

            <div className="flex gap-3">
              <Button type="submit" disabled={mutation.isPending}>
                {mutation.isPending ? "Saving..." : "Save Changes"}
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
