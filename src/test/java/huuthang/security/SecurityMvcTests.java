package huuthang.security;

import huuthang.config.SecurityConfig;
import huuthang.controller.AuthController;
import huuthang.controller.HomeController;
import huuthang.controller.ProductController;
import huuthang.controller.UserController;
import huuthang.dto.ProductDTO;
import huuthang.service.AuthService;
import huuthang.service.ProductService;
import huuthang.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = {
        AuthController.class,
        HomeController.class,
        ProductController.class,
        UserController.class
})
@Import(SecurityConfig.class)
class SecurityMvcTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void publicAuthenticationPagesAreAccessible() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));

        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/forgot-password"));
    }

    @Test
    void anonymousUserIsRedirectedFromProtectedPages() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void normalUserCanOpenProductsButCannotOpenUserManagement() throws Exception {
        when(productService.findAll(anyString(), anyInt(), anyInt())).thenReturn(Page.empty());
        Authentication user = authenticationFor("ROLE_USER");

        mockMvc.perform(get("/products").with(authentication(user)))
                .andExpect(status().isOk())
                .andExpect(view().name("products/list"));

        mockMvc.perform(get("/users").with(authentication(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanOpenUserManagement() throws Exception {
        when(userService.findAll(anyString(), anyInt(), anyInt())).thenReturn(Page.empty());

        mockMvc.perform(get("/users").with(authentication(authenticationFor("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(view().name("users/list"));
    }

    @Test
    void csrfRejectsStateChangingRequestWithoutToken() throws Exception {
        mockMvc.perform(post("/users/delete/1")
                        .with(authentication(authenticationFor("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRequestWithCsrfCanDeleteUser() throws Exception {
        mockMvc.perform(post("/users/delete/1")
                        .with(authentication(authenticationFor("ROLE_ADMIN")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"));

        verify(userService).delete(1L);
    }

    @Test
    void normalUserCannotEditAnotherUsersProduct() throws Exception {
        ProductDTO product = new ProductDTO();
        product.setId(42L);
        product.setUserId(2L);
        when(productService.findById(42L)).thenReturn(product);

        mockMvc.perform(get("/products/edit/42")
                        .with(authentication(authenticationFor("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    private Authentication authenticationFor(String role) {
        CustomUserDetails principal = new CustomUserDetails(
                1L,
                "tester",
                "tester@example.com",
                "{noop}password",
                "Security Tester",
                null,
                role,
                true
        );
        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                principal.getPassword(),
                principal.getAuthorities()
        );
    }
}
