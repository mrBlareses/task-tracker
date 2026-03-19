package org.mushroom.model.task;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.mushroom.model.task.TaskPageResponsePageable;
import org.mushroom.model.task.TaskResponse;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * TaskPageResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-26T17:49:53.554236600+03:00[Europe/Moscow]", comments = "Generator version: 7.13.0")
public class TaskPageResponse {

  @Valid
  private List<@Valid TaskResponse> content = new ArrayList<>();

  private @Nullable TaskPageResponsePageable pageable;

  private @Nullable Long totalElements;

  private @Nullable Integer totalPages;

  public TaskPageResponse content(List<@Valid TaskResponse> content) {
    this.content = content;
    return this;
  }

  public TaskPageResponse addContentItem(TaskResponse contentItem) {
    if (this.content == null) {
      this.content = new ArrayList<>();
    }
    this.content.add(contentItem);
    return this;
  }

  /**
   * Get content
   * @return content
   */
  @Valid 
  @Schema(name = "content", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("content")
  public List<@Valid TaskResponse> getContent() {
    return content;
  }

  public void setContent(List<@Valid TaskResponse> content) {
    this.content = content;
  }

  public TaskPageResponse pageable(TaskPageResponsePageable pageable) {
    this.pageable = pageable;
    return this;
  }

  /**
   * Get pageable
   * @return pageable
   */
  @Valid 
  @Schema(name = "pageable", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("pageable")
  public TaskPageResponsePageable getPageable() {
    return pageable;
  }

  public void setPageable(TaskPageResponsePageable pageable) {
    this.pageable = pageable;
  }

  public TaskPageResponse totalElements(Long totalElements) {
    this.totalElements = totalElements;
    return this;
  }

  /**
   * Get totalElements
   * @return totalElements
   */
  
  @Schema(name = "totalElements", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("totalElements")
  public Long getTotalElements() {
    return totalElements;
  }

  public void setTotalElements(Long totalElements) {
    this.totalElements = totalElements;
  }

  public TaskPageResponse totalPages(Integer totalPages) {
    this.totalPages = totalPages;
    return this;
  }

  /**
   * Get totalPages
   * @return totalPages
   */
  
  @Schema(name = "totalPages", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("totalPages")
  public Integer getTotalPages() {
    return totalPages;
  }

  public void setTotalPages(Integer totalPages) {
    this.totalPages = totalPages;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TaskPageResponse taskPageResponse = (TaskPageResponse) o;
    return Objects.equals(this.content, taskPageResponse.content) &&
        Objects.equals(this.pageable, taskPageResponse.pageable) &&
        Objects.equals(this.totalElements, taskPageResponse.totalElements) &&
        Objects.equals(this.totalPages, taskPageResponse.totalPages);
  }

  @Override
  public int hashCode() {
    return Objects.hash(content, pageable, totalElements, totalPages);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TaskPageResponse {\n");
    sb.append("    content: ").append(toIndentedString(content)).append("\n");
    sb.append("    pageable: ").append(toIndentedString(pageable)).append("\n");
    sb.append("    totalElements: ").append(toIndentedString(totalElements)).append("\n");
    sb.append("    totalPages: ").append(toIndentedString(totalPages)).append("\n");
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

