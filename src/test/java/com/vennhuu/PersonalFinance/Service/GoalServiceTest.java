package com.vennhuu.PersonalFinance.Service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vennhuu.PersonalFinance.Entity.Goal;
import com.vennhuu.PersonalFinance.Entity.Request.Goal.GoalContributeReq;
import com.vennhuu.PersonalFinance.Entity.Request.Goal.GoalReq;
import com.vennhuu.PersonalFinance.Entity.Response.Goal.ResGoal;
import com.vennhuu.PersonalFinance.Entity.Response.ResultPaginationDTO;
import com.vennhuu.PersonalFinance.Entity.User;
import com.vennhuu.PersonalFinance.Exception.BadRequestException;
import com.vennhuu.PersonalFinance.Exception.ResourceNotFoundException;
import com.vennhuu.PersonalFinance.Repository.GoalRepository;
import com.vennhuu.PersonalFinance.Utils.SecurityUtil;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private GoalRepository goalRepository;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private UserService userService;

    @InjectMocks
    private GoalService goalService;

    private User sampleUser;
    private Goal sampleGoal;
    private GoalReq goalReq;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("a@example.com", null));

        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setEmail("a@example.com");

        sampleGoal = new Goal();
        sampleGoal.setId(30L);
        sampleGoal.setUser(sampleUser);
        sampleGoal.setName("Mua xe may");
        sampleGoal.setTargetAmount(BigDecimal.valueOf(30_000_000));
        sampleGoal.setCurrentAmount(BigDecimal.valueOf(10_000_000));
        sampleGoal.setDeadline(LocalDate.of(2027, 1, 1));

        goalReq = new GoalReq();
        goalReq.setName("Mua xe may");
        goalReq.setTargetAmount(BigDecimal.valueOf(30_000_000));
        goalReq.setDeadline(LocalDate.of(2027, 1, 1));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockCurrentUser() {
        when(userService.findByEmail("a@example.com")).thenReturn(sampleUser);
    }

    // createGoal
    @Nested
    @DisplayName("createGoal(GoalReq)")
    class CreateGoal {

        @Test
        @DisplayName("Create goal with currentAmount = 0 and return correct response")
        void shouldCreateGoalWithZeroCurrentAmount() {
            mockCurrentUser();

            Goal savedGoal = new Goal();
            savedGoal.setId(31L);
            savedGoal.setName("Mua xe may");
            savedGoal.setTargetAmount(BigDecimal.valueOf(30_000_000));
            savedGoal.setCurrentAmount(BigDecimal.ZERO);
            savedGoal.setDeadline(LocalDate.of(2027, 1, 1));

            when(goalRepository.save(any(Goal.class))).thenReturn(savedGoal);

            ResGoal result = goalService.createGoal(goalReq);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(31L);
            assertThat(result.getCurrentAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getRemainingAmount()).isEqualByComparingTo(BigDecimal.valueOf(30_000_000));
            assertThat(result.getPercentAchieved()).isEqualTo(0.0);
            assertThat(result.isCompleted()).isFalse();
        }
    }

    // convertToResGoal – edge cases
    @Nested
    @DisplayName("convertToResGoal – edge cases via getDetailGoal")
    class ConvertToResGoal {

        @Test
        @DisplayName("remaining = 0 (not negative) when currentAmount exceeds targetAmount")
        void shouldCapRemainingAtZeroWhenOverfunded() {
            mockCurrentUser();
            sampleGoal.setCurrentAmount(BigDecimal.valueOf(35_000_000)); // > target
            when(goalRepository.findByIdAndUser_Id(30L, 1L)).thenReturn(Optional.of(sampleGoal));

            ResGoal result = goalService.getDetailGoal(30L);

            assertThat(result.getRemainingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getPercentAchieved()).isEqualTo(100.0);
            assertThat(result.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("percentUsed = 0 when targetAmount is zero")
        void shouldReturnZeroPercentWhenTargetIsZero() {
            mockCurrentUser();
            sampleGoal.setTargetAmount(BigDecimal.ZERO);
            sampleGoal.setCurrentAmount(BigDecimal.ZERO);
            when(goalRepository.findByIdAndUser_Id(30L, 1L)).thenReturn(Optional.of(sampleGoal));

            ResGoal result = goalService.getDetailGoal(30L);

            assertThat(result.getPercentAchieved()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Normal case: 10M / 30M = 33.33% and not completed")
        void shouldComputeCorrectPercentAndNotCompleted() {
            mockCurrentUser();
            when(goalRepository.findByIdAndUser_Id(30L, 1L)).thenReturn(Optional.of(sampleGoal));

            ResGoal result = goalService.getDetailGoal(30L);

            assertThat(result.getPercentAchieved()).isEqualTo(33.33, within(0.01));
            assertThat(result.getRemainingAmount()).isEqualByComparingTo(BigDecimal.valueOf(20_000_000));
            assertThat(result.isCompleted()).isFalse();
        }
    }

    // getAllGoalByUser
    @Nested
    @DisplayName("getAllGoalByUser(Pageable)")
    class GetAllGoalByUser {

        @Test
        @DisplayName("Return paginated goals for current user")
        void shouldReturnPaginatedGoals() {
            mockCurrentUser();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Goal> page = new PageImpl<>(List.of(sampleGoal), pageable, 1);

            when(goalRepository.getAllBudgetsByUserId(1L, pageable)).thenReturn(page);

            ResultPaginationDTO result = goalService.getAllGoalByUser(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getMeta().getTotalElements()).isEqualTo(1);
            assertThat(result.getMeta().getCurrentPage()).isEqualTo(1);

            @SuppressWarnings("unchecked")
            List<ResGoal> goals = (List<ResGoal>) result.getResult();
            assertThat(goals).hasSize(1);
        }
    }

    // getDetailGoal
    @Nested
    @DisplayName("getDetailGoal(Long)")
    class GetDetailGoal {

        @Test
        @DisplayName("Return goal details when found")
        void shouldReturnGoalDetails() {
            mockCurrentUser();
            when(goalRepository.findByIdAndUser_Id(30L, 1L)).thenReturn(Optional.of(sampleGoal));

            ResGoal result = goalService.getDetailGoal(30L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(30L);
            assertThat(result.getName()).isEqualTo("Mua xe may");
        }

        @Test
        @DisplayName("Throw ResourceNotFoundException when goal not found")
        void shouldThrowWhenNotFound() {
            mockCurrentUser();
            when(goalRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> goalService.getDetailGoal(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // updateGoal
    @Nested
    @DisplayName("updateGoal(Long, GoalReq)")
    class UpdateGoal {

        @Test
        @DisplayName("Throw BadRequestException when new target < current amount")
        void shouldThrowWhenTargetLessThanCurrent() {
            mockCurrentUser();
            when(goalRepository.findByIdAndUser_Id(30L, 1L)).thenReturn(Optional.of(sampleGoal));
            // currentAmount = 10M, setting target to 5M
            goalReq.setTargetAmount(BigDecimal.valueOf(5_000_000));

            assertThatThrownBy(() -> goalService.updateGoal(30L, goalReq))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("không được nhỏ hơn");
        }

        @Test
        @DisplayName("Update goal successfully when new target >= current amount")
        void shouldUpdateGoalSuccessfully() {
            mockCurrentUser();
            when(goalRepository.findByIdAndUser_Id(30L, 1L)).thenReturn(Optional.of(sampleGoal));

            Goal updated = new Goal();
            updated.setId(30L);
            updated.setName("Mua xe may moi");
            updated.setTargetAmount(BigDecimal.valueOf(40_000_000));
            updated.setCurrentAmount(BigDecimal.valueOf(10_000_000));
            updated.setDeadline(LocalDate.of(2027, 6, 1));

            when(goalRepository.save(any(Goal.class))).thenReturn(updated);

            goalReq.setName("Mua xe may moi");
            goalReq.setTargetAmount(BigDecimal.valueOf(40_000_000));

            ResGoal result = goalService.updateGoal(30L, goalReq);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Mua xe may moi");
            assertThat(result.getTargetAmount()).isEqualByComparingTo(BigDecimal.valueOf(40_000_000));
        }

        @Test
        @DisplayName("Throw ResourceNotFoundException when goal not found")
        void shouldThrowWhenNotFound() {
            mockCurrentUser();
            when(goalRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> goalService.updateGoal(99L, goalReq))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // deleteGoal
    @Nested
    @DisplayName("deleteGoal(Long)")
    class DeleteGoal {

        @Test
        @DisplayName("Delete goal when found for user")
        void shouldDeleteGoalSuccessfully() {
            mockCurrentUser();
            when(goalRepository.findByIdAndUser_Id(30L, 1L)).thenReturn(Optional.of(sampleGoal));

            goalService.deleteGoal(30L);

            verify(goalRepository).delete(sampleGoal);
        }

        @Test
        @DisplayName("Throw ResourceNotFoundException when goal not found")
        void shouldThrowWhenNotFound() {
            mockCurrentUser();
            when(goalRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> goalService.deleteGoal(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // contributeToGoal
    @Nested
    @DisplayName("contributeToGoal(Long, GoalContributeReq)")
    class ContributeToGoal {

        @Test
        @DisplayName("Add contribution amount to currentAmount")
        void shouldAddContributionToCurrentAmount() {
            mockCurrentUser();
            when(goalRepository.findByIdAndUser_Id(30L, 1L)).thenReturn(Optional.of(sampleGoal));

            Goal afterContribute = new Goal();
            afterContribute.setId(30L);
            afterContribute.setName("Mua xe may");
            afterContribute.setTargetAmount(BigDecimal.valueOf(30_000_000));
            afterContribute.setCurrentAmount(BigDecimal.valueOf(15_000_000)); // 10M + 5M
            afterContribute.setDeadline(LocalDate.of(2027, 1, 1));

            when(goalRepository.save(any(Goal.class))).thenReturn(afterContribute);

            GoalContributeReq req = new GoalContributeReq(BigDecimal.valueOf(5_000_000));
            ResGoal result = goalService.contributeToGoal(30L, req);

            assertThat(result).isNotNull();
            assertThat(result.getCurrentAmount()).isEqualByComparingTo(BigDecimal.valueOf(15_000_000));
            // should upd currentAmount on entity before save
            assertThat(sampleGoal.getCurrentAmount()).isEqualByComparingTo(BigDecimal.valueOf(15_000_000));
        }

        @Test
        @DisplayName("Mark goal as completed when contribution brings total >= target")
        void shouldMarkGoalCompletedWhenFunded() {
            mockCurrentUser();
            // currentAmount = 10M, contributing 20M → total 30M = target
            when(goalRepository.findByIdAndUser_Id(30L, 1L)).thenReturn(Optional.of(sampleGoal));

            Goal completedGoal = new Goal();
            completedGoal.setId(30L);
            completedGoal.setName("Mua xe may");
            completedGoal.setTargetAmount(BigDecimal.valueOf(30_000_000));
            completedGoal.setCurrentAmount(BigDecimal.valueOf(30_000_000));
            completedGoal.setDeadline(LocalDate.of(2027, 1, 1));

            when(goalRepository.save(any(Goal.class))).thenReturn(completedGoal);

            GoalContributeReq req = new GoalContributeReq(BigDecimal.valueOf(20_000_000));
            ResGoal result = goalService.contributeToGoal(30L, req);

            assertThat(result.isCompleted()).isTrue();
            assertThat(result.getPercentAchieved()).isEqualTo(100.0);
            assertThat(result.getRemainingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Throw ResourceNotFoundException when goal not found")
        void shouldThrowWhenGoalNotFound() {
            mockCurrentUser();
            when(goalRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());

            GoalContributeReq req = new GoalContributeReq(BigDecimal.valueOf(1_000_000));

            assertThatThrownBy(() -> goalService.contributeToGoal(99L, req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
