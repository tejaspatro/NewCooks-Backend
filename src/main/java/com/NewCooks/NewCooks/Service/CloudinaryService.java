package com.NewCooks.NewCooks.Service;

import com.NewCooks.NewCooks.Entity.Recipe;
import com.NewCooks.NewCooks.Repository.RecipeRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CloudinaryService
{
    private final Cloudinary cloudinary;
    private final RecipeRepository recipeRepository;

    public void deleteImageFromCloud(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Recipe removeImage(Long recipeId, String urlToRemove) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        boolean wasThumbnail = urlToRemove.equals(recipe.getThumbnail());

        if (wasThumbnail) {
            String publicId = extractPublicId(urlToRemove);
            deleteImageFromCloud(publicId);
            recipe.setThumbnail(null); // Set thumbnail to null
        } else if (recipe.getImages().contains(urlToRemove)) {
            recipe.getImages().remove(urlToRemove);
            String publicId = extractPublicId(urlToRemove);
            deleteImageFromCloud(publicId);
        }

        return recipeRepository.save(recipe);
    }

    public String extractPublicId(String url) {
        // Cloudinary URL example: https://res.cloudinary.com/<cloud>/image/upload/v123456/<public_id>.jpg
        String[] parts = url.split("/");
        String filename = parts[parts.length - 1]; // "<public_id>.jpg"
        return filename.substring(0, filename.lastIndexOf('.')); // "<public_id>"
    }
}
