package com.vennhuu.PersonalFinance.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

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

@Service
public class GoalService {

    private final GoalRepository goalRepository;
    private final SecurityUtil securityUtil;
    private final UserService userService;

    public GoalService(GoalRepository goalRepository, SecurityUtil securityUtil, UserService userService) {
        this.goalRepository = goalRepository;
        this.securityUtil = securityUtil;
        this.userService = userService;
    }

    private User getCurrentUser() {
        String email = securityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new BadCredentialsException("Vui lòng đăng nhập"));
        return userService.findByEmail(email);
    }

    private Goal getOwnedGoal(Long id, Long userId) {
        return goalRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mục tiêu"));
    }

    private void applyGoalData(Goal goal, GoalReq req) {
        goal.setName(req.getName());
        goal.setTargetAmount(req.getTargetAmount());
        goal.setDeadline(req.getDeadline());
    }

    // convert res

    private ResGoal convertToResGoal(Goal goal) {
        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount());
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO; // không hiển thị âm nếu lỡ nạp vượt mục tiêu
        }

        double percent = goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0
                ? goal.getCurrentAmount().divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;
        if (percent > 100) percent = 100; // chặn hiển thị vượt quá 100%

        boolean completed = goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0;

        return new ResGoal(
                goal.getId(),
                goal.getName(),
                goal.getTargetAmount(),
                goal.getCurrentAmount(),
                remaining,
                percent,
                completed,
                goal.getDeadline()
        );
    }

    // api
    public ResGoal createGoal(GoalReq req) {
        User user = getCurrentUser();

        Goal goal = new Goal();
        goal.setUser(user);
        applyGoalData(goal, req);
        goal.setCurrentAmount(BigDecimal.ZERO); // luôn bắt đầu từ 0

        Goal saved = goalRepository.save(goal);
        return convertToResGoal(saved);
    }

    public ResultPaginationDTO getAllGoalByUser(Pageable pageable) {
        User user = this.getCurrentUser();
        
        Page<Goal> pageGoal = this.goalRepository.getAllBudgetsByUserId(user.getId(), pageable) ;

        ResultPaginationDTO res = new ResultPaginationDTO() ;

        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setCurrentPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotalElements(pageGoal.getTotalElements());
        meta.setTotalPages(pageGoal.getTotalPages());

        List<ResGoal> listBudget = pageGoal.getContent().stream().map(this::convertToResGoal).toList();

        res.setMeta(meta);
        res.setResult(listBudget);

        return res ;
    }

    public ResGoal getDetailGoal(Long id) {
        User user = getCurrentUser();
        Goal goal = getOwnedGoal(id, user.getId());
        return convertToResGoal(goal);
    }

    public ResGoal updateGoal(Long id, GoalReq req) {
        User user = getCurrentUser();
        Goal goal = getOwnedGoal(id, user.getId());

        // Không cho đặt targetAmount mới thấp hơn số tiền đã tích luỹ, tránh vô lý
        if (req.getTargetAmount().compareTo(goal.getCurrentAmount()) < 0) {
            throw new BadRequestException("Số tiền mục tiêu không được nhỏ hơn số tiền đã tích luỹ hiện tại");
        }

        applyGoalData(goal, req);
        Goal updated = goalRepository.save(goal);

        return convertToResGoal(updated);
    }

    public void deleteGoal(Long id) {
        User user = getCurrentUser();
        Goal goal = getOwnedGoal(id, user.getId());
        goalRepository.delete(goal);
    }

    // Nạp thêm tiền vào mục tiêu
    public ResGoal contributeToGoal(Long id, GoalContributeReq req) {
        User user = getCurrentUser();
        Goal goal = getOwnedGoal(id, user.getId());

        BigDecimal newAmount = goal.getCurrentAmount().add(req.getAmount());
        goal.setCurrentAmount(newAmount);

        Goal updated = goalRepository.save(goal);
        return convertToResGoal(updated);
    }
}