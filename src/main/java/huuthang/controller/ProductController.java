package huuthang.controller;

import huuthang.dto.ProductDTO;
import huuthang.security.CustomUserDetails;
import huuthang.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    String list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        model.addAttribute("products", productService.findAll(keyword, page, size));
        model.addAttribute("keyword", keyword);
        model.addAttribute("size", size);
        return "products/list";
    }

    @GetMapping("/create")
    String create(Model model) {
        model.addAttribute("productDTO", new ProductDTO());
        model.addAttribute("mode", "create");
        return "products/form";
    }

    @PostMapping("/create")
    String create(
            @Valid @ModelAttribute ProductDTO productDTO,
            BindingResult result,
            @RequestParam(name = "image", required = false) MultipartFile image,
            Authentication authentication,
            Model model,
            RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("mode", "create");
            return "products/form";
        }
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        productDTO.setUserId(user.getId());
        try {
            productService.create(productDTO, image);
            redirect.addFlashAttribute("success", "Tạo sản phẩm thành công.");
            return "redirect:/products";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            result.reject("product.error", exception.getMessage());
            model.addAttribute("mode", "create");
            return "products/form";
        }
    }

    @GetMapping("/edit/{id}")
    String edit(@PathVariable Long id, Authentication authentication, Model model) {
        ProductDTO product = productService.findById(id);
        verifyOwnerOrAdmin(product, authentication);
        model.addAttribute("productDTO", product);
        model.addAttribute("mode", "edit");
        return "products/form";
    }

    @PostMapping("/edit/{id}")
    String edit(
            @PathVariable Long id,
            @Valid @ModelAttribute ProductDTO productDTO,
            BindingResult result,
            @RequestParam(name = "image", required = false) MultipartFile image,
            Authentication authentication,
            Model model,
            RedirectAttributes redirect) {
        verifyOwnerOrAdmin(productService.findById(id), authentication);
        if (result.hasErrors()) {
            model.addAttribute("mode", "edit");
            return "products/form";
        }
        try {
            productService.update(id, productDTO, image);
            redirect.addFlashAttribute("success", "Cập nhật sản phẩm thành công.");
            return "redirect:/products";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            result.reject("product.error", exception.getMessage());
            model.addAttribute("mode", "edit");
            return "products/form";
        }
    }

    @PostMapping("/delete/{id}")
    String delete(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
        verifyOwnerOrAdmin(productService.findById(id), authentication);
        productService.delete(id);
        redirect.addFlashAttribute("success", "Xóa sản phẩm thành công.");
        return "redirect:/products";
    }

    private void verifyOwnerOrAdmin(ProductDTO product, Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        if (!admin && !user.getId().equals(product.getUserId())) {
            throw new AccessDeniedException("Bạn không có quyền thao tác sản phẩm này");
        }
    }
}
