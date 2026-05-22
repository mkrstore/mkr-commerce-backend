package com.mkr.commerce.catalog.service;

import com.mkr.commerce.catalog.dto.product.*;
import com.mkr.commerce.catalog.entity.*;
import com.mkr.commerce.catalog.enums.MediaType;
import com.mkr.commerce.catalog.enums.ProductStatus;
import com.mkr.commerce.catalog.repository.*;
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

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository             productRepo;
    private final CategoryRepository            categoryRepo;
    private final BrandRepository               brandRepo;
    private final TagRepository                 tagRepo;
    private final ProductImageRepository        imageRepo;
    private final ProductVariantRepository      variantRepo;
    private final ProductAttributeRepository    attributeRepo;
    private final AttributeDefinitionRepository attrDefRepo;
    private final CloudinaryService             cloudinary;

    // ── List ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ProductSummaryDto> list(UUID categoryId, UUID brandId, ProductStatus status,
                                        String search, Pageable pageable) {
        String searchPattern = (search != null && !search.isBlank())
                ? "%" + search.toLowerCase().trim() + "%" : null;
        return productRepo.findFiltered(categoryId, brandId, status, searchPattern, pageable)
                .map(ProductSummaryDto::from);
    }

    // ── Get ───────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ProductDetailDto get(UUID id) {
        return ProductDetailDto.from(findById(id));
    }

    // ── Create (Step 1 — Draft) ────────────────────────────────────────────────

    @Transactional
    public ProductDetailDto create(CreateProductRequest req) {
        if (productRepo.existsBySku(req.sku())) {
            throw new BadRequestException("SKU '" + req.sku() + "' is already in use.");
        }

        String slug = resolveSlug(req.slug(), req.name());
        if (productRepo.existsBySlug(slug)) {
            throw new BadRequestException("A product with slug '" + slug + "' already exists.");
        }

        Category category = categoryRepo.findById(req.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));
        Brand brand = req.brandId() != null
                ? brandRepo.findById(req.brandId()).orElseThrow(() -> new ResourceNotFoundException("Brand not found."))
                : null;

        Product product = Product.builder()
                .name(req.name().trim())
                .slug(slug)
                .sku(req.sku().trim().toUpperCase())
                .shortDescription(req.shortDescription())
                .description(req.description())
                .barcode(req.barcode())
                .category(category)
                .brand(brand)
                .priceRetail(BigDecimal.ZERO)
                .gstPercent(BigDecimal.valueOf(18))
                .status(ProductStatus.DRAFT)
                .build();

        Product saved = productRepo.save(product);
        log.info("Product created: '{}' [{}]", saved.getName(), saved.getId());
        return ProductDetailDto.from(saved);
    }

    // ── Update (PATCH-style) ───────────────────────────────────────────────────

    @Transactional
    public ProductDetailDto update(UUID id, UpdateProductRequest req) {
        Product product = findById(id);

        if (req.name() != null) {
            product.setName(req.name().trim());
        }
        if (req.slug() != null) {
            String slug = SlugUtils.toSlug(req.slug());
            if (productRepo.existsBySlugAndIdNot(slug, id)) {
                throw new BadRequestException("A product with slug '" + slug + "' already exists.");
            }
            product.setSlug(slug);
        }
        if (req.sku() != null) {
            if (productRepo.existsBySkuAndIdNot(req.sku(), id)) {
                throw new BadRequestException("SKU '" + req.sku() + "' is already in use.");
            }
            product.setSku(req.sku().trim().toUpperCase());
        }
        if (req.shortDescription() != null) product.setShortDescription(req.shortDescription());
        if (req.description()      != null) product.setDescription(req.description());
        if (req.barcode()          != null) product.setBarcode(req.barcode().trim());

        if (req.categoryId() != null) {
            Category category = categoryRepo.findById(req.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found."));
            product.setCategory(category);
        }
        if (req.brandId() != null) {
            Brand brand = brandRepo.findById(req.brandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Brand not found."));
            product.setBrand(brand);
        }

        if (req.priceRetail()     != null) product.setPriceRetail(req.priceRetail());
        if (req.priceWholesale()  != null) product.setPriceWholesale(req.priceWholesale());
        if (req.priceBroker()     != null) product.setPriceBroker(req.priceBroker());
        if (req.minQtyWholesale() != null) product.setMinQtyWholesale(req.minQtyWholesale());
        if (req.gstPercent()      != null) product.setGstPercent(req.gstPercent());
        if (req.gstIncluded()     != null) product.setGstIncluded(req.gstIncluded());
        if (req.stockQty()        != null) product.setStockQty(req.stockQty());
        if (req.weight()          != null) product.setWeight(req.weight());
        if (req.dimensions()      != null) product.setDimensions(req.dimensions());
        if (req.status()          != null) product.setStatus(req.status());

        if (req.tags() != null) {
            product.getTags().clear();
            applyTags(product, req.tags());
        }

        Product saved = productRepo.save(product);
        log.info("Product updated: '{}' [{}]", saved.getName(), id);
        return ProductDetailDto.from(saved);
    }

    // ── Deactivate (soft delete) ──────────────────────────────────────────────

    @Transactional
    public void deactivate(UUID id) {
        Product product = findById(id);
        product.setStatus(ProductStatus.INACTIVE);
        productRepo.save(product);
        log.info("Product deactivated: '{}' [{}]", product.getName(), id);
    }

    // ── Save Attributes (replace all) ────────────────────────────────────────

    @Transactional
    public List<ProductAttributeDto> saveAttributes(UUID productId, SaveAttributesRequest req) {
        Product product = findById(productId);

        attributeRepo.deleteAllByProductId(productId);

        List<ProductAttribute> saved = new ArrayList<>();
        for (SaveAttributesRequest.AttributeEntry entry : req.attributes()) {
            AttributeDefinition def = attrDefRepo.findById(entry.definitionId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Attribute definition not found: " + entry.definitionId()));

            saved.add(attributeRepo.save(ProductAttribute.builder()
                    .product(product)
                    .definition(def)
                    .value(entry.value())
                    .build()));
        }

        return saved.stream().map(ProductAttributeDto::from).toList();
    }

    // ── Upload Media ──────────────────────────────────────────────────────────

    @Transactional
    public ProductImageDto uploadMedia(UUID productId, MultipartFile file,
                                       MediaType mediaType, String altText) {
        Product product = findById(productId);

        MediaType resolvedType = mediaType != null ? mediaType
                : (product.getImages().isEmpty() ? MediaType.IMAGE_PRIMARY : MediaType.IMAGE_GALLERY);

        boolean isVideo = resolvedType == MediaType.VIDEO;
        CloudinaryService.UploadResult result =
                cloudinary.upload(file, "mkr-commerce/products/" + productId, isVideo);

        if (resolvedType == MediaType.IMAGE_PRIMARY) {
            imageRepo.clearPrimaryForProduct(productId);
        }

        int sortOrder = imageRepo.countByProductId(productId);
        ProductImage image = ProductImage.builder()
                .product(product)
                .mediaType(resolvedType)
                .url(result.url())
                .publicId(result.publicId())
                .altText(altText)
                .sortOrder(sortOrder)
                .build();

        ProductImage saved = imageRepo.save(image);
        log.info("Media uploaded for product [{}]: {} ({})", productId, result.publicId(), resolvedType);
        return ProductImageDto.from(saved);
    }

    // ── Delete Media ──────────────────────────────────────────────────────────

    @Transactional
    public void deleteMedia(UUID productId, UUID imageId) {
        ProductImage image = imageRepo.findById(imageId)
                .filter(i -> i.getProduct().getId().equals(productId))
                .orElseThrow(() -> new ResourceNotFoundException("Media not found."));

        boolean wasPrimary = image.isPrimary();
        String publicId = image.getPublicId();
        cloudinary.delete(publicId);
        imageRepo.delete(image);
        log.info("Media deleted for product [{}]: {}", productId, publicId);

        if (wasPrimary) {
            imageRepo.findAllByProductIdOrderBySortOrderAsc(productId)
                    .stream().findFirst().ifPresent(next -> {
                        next.setMediaType(MediaType.IMAGE_PRIMARY);
                        imageRepo.save(next);
                    });
        }
    }

    // ── Clone ─────────────────────────────────────────────────────────────────

    @Transactional
    public ProductDetailDto clone(UUID sourceId) {
        Product source = findById(sourceId);

        String baseSlug = source.getSlug() + "-copy";
        String slug = baseSlug;
        for (int i = 1; productRepo.existsBySlug(slug); i++) {
            slug = baseSlug + "-" + i;
        }

        String baseSku = source.getSku() + "-COPY";
        String sku = baseSku;
        for (int i = 1; productRepo.existsBySku(sku); i++) {
            sku = baseSku + i;
        }

        Product copy = Product.builder()
                .name(source.getName() + " (Copy)")
                .slug(slug)
                .sku(sku)
                .shortDescription(source.getShortDescription())
                .description(source.getDescription())
                .barcode(null)
                .category(source.getCategory())
                .brand(source.getBrand())
                .priceRetail(source.getPriceRetail())
                .priceWholesale(source.getPriceWholesale())
                .priceBroker(source.getPriceBroker())
                .minQtyWholesale(source.getMinQtyWholesale())
                .gstPercent(source.getGstPercent())
                .gstIncluded(source.isGstIncluded())
                .stockQty(0)
                .weight(source.getWeight())
                .dimensions(source.getDimensions())
                .status(ProductStatus.DRAFT)
                .build();

        copy.getTags().addAll(source.getTags());
        Product saved = productRepo.save(copy);

        for (ProductAttribute attr : source.getAttributes()) {
            ProductAttribute newAttr = attributeRepo.save(ProductAttribute.builder()
                    .product(saved)
                    .definition(attr.getDefinition())
                    .value(attr.getValue())
                    .build());
            saved.getAttributes().add(newAttr);
        }

        // Copy image records referencing same URLs (publicId = null to avoid Cloudinary conflicts)
        for (ProductImage img : source.getImages()) {
            ProductImage newImg = imageRepo.save(ProductImage.builder()
                    .product(saved)
                    .mediaType(img.getMediaType())
                    .url(img.getUrl())
                    .publicId(null)
                    .altText(img.getAltText())
                    .sortOrder(img.getSortOrder())
                    .build());
            saved.getImages().add(newImg);
        }

        log.info("Product cloned: '{}' → '{}' [{}]", source.getName(), saved.getName(), saved.getId());
        return ProductDetailDto.from(saved);
    }

    // ── Variants ──────────────────────────────────────────────────────────────

    @Transactional
    public ProductVariantDto addVariant(UUID productId, CreateVariantRequest req) {
        Product product = findById(productId);
        if (variantRepo.existsBySku(req.sku())) {
            throw new BadRequestException("Variant SKU '" + req.sku() + "' is already in use.");
        }
        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .sku(req.sku().trim().toUpperCase())
                .colorName(req.colorName())
                .colorHex(req.colorHex())
                .size(req.size())
                .priceOverride(req.priceOverride())
                .stockQty(req.stockQty())
                .isActive(true)
                .build();
        ProductVariant saved = variantRepo.save(variant);
        log.info("Variant added to product [{}]: SKU {}", productId, saved.getSku());
        return ProductVariantDto.from(saved);
    }

    @Transactional
    public ProductVariantDto updateVariant(UUID productId, UUID variantId, UpdateVariantRequest req) {
        ProductVariant variant = variantRepo.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found."));
        if (variantRepo.existsBySkuAndIdNot(req.sku(), variantId)) {
            throw new BadRequestException("Variant SKU '" + req.sku() + "' is already in use.");
        }
        variant.setSku(req.sku().trim().toUpperCase());
        variant.setColorName(req.colorName());
        variant.setColorHex(req.colorHex());
        variant.setSize(req.size());
        variant.setPriceOverride(req.priceOverride());
        variant.setStockQty(req.stockQty());
        variant.setActive(req.isActive());
        return ProductVariantDto.from(variantRepo.save(variant));
    }

    @Transactional
    public void deleteVariant(UUID productId, UUID variantId) {
        ProductVariant variant = variantRepo.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found."));
        variantRepo.delete(variant);
        log.info("Variant deleted from product [{}]: SKU {}", productId, variant.getSku());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Product findById(UUID id) {
        return productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private String resolveSlug(String rawSlug, String name) {
        String slug = (rawSlug != null && !rawSlug.isBlank()) ? rawSlug : SlugUtils.toSlug(name);
        if (slug == null || slug.isBlank()) throw new BadRequestException("Could not generate a valid slug.");
        return slug;
    }

    private void applyTags(Product product, Set<String> tagNames) {
        if (tagNames == null) return;
        for (String name : tagNames) {
            String slug = SlugUtils.toSlug(name);
            Tag tag = tagRepo.findBySlug(slug).orElseGet(() ->
                    tagRepo.save(Tag.builder().name(name.trim()).slug(slug).build()));
            product.getTags().add(tag);
        }
    }
}
