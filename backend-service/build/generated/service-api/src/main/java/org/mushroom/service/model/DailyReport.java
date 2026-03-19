package org.mushroom.service.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * DailyReport
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-26T17:49:53.478770700+03:00[Europe/Moscow]", comments = "Generator version: 7.13.0")
public class DailyReport {

  private UUID userId;

  private String email;

  private Integer pendingCount;

  private Integer completedCount;

  public DailyReport() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public DailyReport(UUID userId, String email, Integer pendingCount, Integer completedCount) {
    this.userId = userId;
    this.email = email;
    this.pendingCount = pendingCount;
    this.completedCount = completedCount;
  }

  public DailyReport userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * User identifier.
   * @return userId
   */
  @NotNull @Valid 
  @Schema(name = "userId", description = "User identifier.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("userId")
  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public DailyReport email(String email) {
    this.email = email;
    return this;
  }

  /**
   * User email.
   * @return email
   */
  @NotNull @jakarta.validation.constraints.Email 
  @Schema(name = "email", description = "User email.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("email")
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public DailyReport pendingCount(Integer pendingCount) {
    this.pendingCount = pendingCount;
    return this;
  }

  /**
   * Count of tasks with status TODO or IN_PROGRESS.
   * @return pendingCount
   */
  @NotNull 
  @Schema(name = "pendingCount", description = "Count of tasks with status TODO or IN_PROGRESS.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("pendingCount")
  public Integer getPendingCount() {
    return pendingCount;
  }

  public void setPendingCount(Integer pendingCount) {
    this.pendingCount = pendingCount;
  }

  public DailyReport completedCount(Integer completedCount) {
    this.completedCount = completedCount;
    return this;
  }

  /**
   * Count of tasks marked DONE during [from, to).
   * @return completedCount
   */
  @NotNull 
  @Schema(name = "completedCount", description = "Count of tasks marked DONE during [from, to).", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("completedCount")
  public Integer getCompletedCount() {
    return completedCount;
  }

  public void setCompletedCount(Integer completedCount) {
    this.completedCount = completedCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DailyReport dailyReport = (DailyReport) o;
    return Objects.equals(this.userId, dailyReport.userId) &&
        Objects.equals(this.email, dailyReport.email) &&
        Objects.equals(this.pendingCount, dailyReport.pendingCount) &&
        Objects.equals(this.completedCount, dailyReport.completedCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, email, pendingCount, completedCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DailyReport {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    pendingCount: ").append(toIndentedString(pendingCount)).append("\n");
    sb.append("    completedCount: ").append(toIndentedString(completedCount)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

