package huuthang.service.impl;

import huuthang.dto.ProductDTO;
import huuthang.entity.Product;
import huuthang.entity.User;
import huuthang.mapper.ProductMapper;
import huuthang.repository.ProductRepository;
import huuthang.repository.UserRepository;
import huuthang.service.CloudinaryService;
import huuthang.service.CloudinaryUploadResult;
import huuthang.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductMapper mapper;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDTO> findAll(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "id"));
        return productRepository.search(keyword == null ? "" : keyword.trim(), pageable)
                .map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDTO findById(Long id) {
        return mapper.toDTO(findProduct(id));
    }

    @Override
    @Transactional
    public ProductDTO create(ProductDTO dto, MultipartFile image) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));
        Product product = mapper.toEntity(dto);
        product.setName(dto.getName().trim());
        product.setUser(user);
        if (image != null && !image.isEmpty()) {
            CloudinaryUploadResult uploaded = cloudinaryService.upload(image);
            product.setImageUrl(uploaded.url() + "|" + uploaded.publicId());
        }
        return mapper.toDTO(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductDTO update(Long id, ProductDTO dto, MultipartFile image) {
        Product product = findProduct(id);
        product.setName(dto.getName().trim());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());

        if (image != null && !image.isEmpty()) {
            deleteCloudinaryImage(product.getImageUrl());
            CloudinaryUploadResult uploaded = cloudinaryService.upload(image);
            product.setImageUrl(uploaded.url() + "|" + uploaded.publicId());
        }
        return mapper.toDTO(productRepository.save(product));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = findProduct(id);
        deleteCloudinaryImage(product.getImageUrl());
        productRepository.delete(product);
    }

    @Override
    @Transactional(readOnly = true)
    public long countProducts() {
        return productRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByUser(Long userId) {
        return productRepository.countByUserId(userId);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product không tồn tại"));
    }

    private void deleteCloudinaryImage(String imageUrl) {
        if (imageUrl != null && imageUrl.contains("|")) {
            cloudinaryService.delete(imageUrl.substring(imageUrl.indexOf('|') + 1));
        }
    }
}
