package com.mkr.commerce.catalog.service;

import com.mkr.commerce.catalog.dto.brand.*;
import com.mkr.commerce.catalog.entity.Brand;
import com.mkr.commerce.catalog.repository.BrandRepository;
import com.mkr.commerce.catalog.repository.ProductRepository;
import com.mkr.commerce.common.exception.BadRequestException;
import com.mkr.commerce.common.exception.ResourceNotFoundException;
import com.mkr.commerce.common.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository   brandRepo;
    private final ProductRepository productRepo;
    private final CloudinaryService  cloudinary;

    @Transactional(readOnly = true)
    public Page<BrandDto> list(Boolean active, String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return brandRepo.search(search, pageable).map(BrandDto::from);
        }
        if (active != null) {
            return brandRepo.findAllByIsActive(active, pageable).map(BrandDto::from);
        }
        return brandRepo.findAll(pageable).map(BrandDto::from);
    }

    @Transactional(readOnly = true)
    public BrandDto get(UUID id) {
        return BrandDto.from(findById(id));
    }

    @Transactional
    public BrandDto create(CreateBrandRequest req) {
        String slug = resolveSlug(req.slug(), req.name());
        if (brandRepo.existsBySlug(slug)) {
            throw new BadRequestException("A brand with slug '" + slug + "' already exists.");
        }
        Brand brand = Brand.builder()
                .name(req.name().trim())
                .slug(slug)
                .description(req.description())
                .isActive(true)
                .build();
        Brand saved = brandRepo.save(brand);
        log.info("Brand created: '{}' [{}]", saved.getName(), saved.getId());
        return BrandDto.from(saved);
    }

    @Transactional
    public BrandDto update(UUID id, UpdateBrandRequest req) {
        Brand brand = findById(id);
        String slug = resolveSlug(req.slug(), req.name());
        if (brandRepo.existsBySlugAndIdNot(slug, id)) {
            throw new BadRequestException("A brand with slug '" + slug + "' already exists.");
        }
        if (brandRepo.existsByNameAndIdNot(req.name().trim(), id)) {
            throw new BadRequestException("A brand named '" + req.name() + "' already exists.");
        }
        brand.setName(req.name().trim());
        brand.setSlug(slug);
        brand.setDescription(req.description());
        brand.setActive(req.isActive());
        Brand saved = brandRepo.save(brand);
        log.info("Brand updated: '{}' [{}]", saved.getName(), id);
        return BrandDto.from(saved);
    }

    @Transactional
    public BrandDto uploadLogo(UUID id, MultipartFile file, boolean isVideo) {
        Brand brand = findById(id);
        if (brand.getLogoPublicId() != null) {
            cloudinary.delete(brand.getLogoPublicId());
        }
        CloudinaryService.UploadResult result = cloudinary.upload(file, "mkr-commerce/brands", isVideo);
        brand.setLogoUrl(result.url());
        brand.setLogoPublicId(result.publicId());
        brand.setLogoIsVideo(isVideo);
        log.info("Logo uploaded for brand '{}' [{}]: {}", brand.getName(), id, result.publicId());
        return BrandDto.from(brandRepo.save(brand));
    }

    @Transactional
    public BrandDto removeLogo(UUID id) {
        Brand brand = findById(id);
        if (brand.getLogoPublicId() != null) {
            cloudinary.delete(brand.getLogoPublicId());
        }
        brand.setLogoUrl(null);
        brand.setLogoPublicId(null);
        brand.setLogoIsVideo(false);
        log.info("Logo removed for brand '{}' [{}]", brand.getName(), id);
        return BrandDto.from(brandRepo.save(brand));
    }

    @Transactional
    public void delete(UUID id) {
        Brand brand = findById(id);
        if (productRepo.existsByBrandId(id)) {
            throw new BadRequestException("Cannot delete a brand that has products assigned to it.");
        }
        brand.setActive(false);
        brandRepo.save(brand);
        log.info("Brand deactivated: '{}' [{}]", brand.getName(), id);
    }

    private Brand findById(UUID id) {
        return brandRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found: " + id));
    }

    private String resolveSlug(String rawSlug, String name) {
        String slug = (rawSlug != null && !rawSlug.isBlank()) ? rawSlug : SlugUtils.toSlug(name);
        if (slug == null || slug.isBlank()) throw new BadRequestException("Could not generate a valid slug.");
        return slug;
    }
}
