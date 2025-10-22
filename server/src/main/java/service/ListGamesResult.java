package service;

import java.util.List;
import java.util.Map;

public record ListGamesResult(List<Map<String, Object>> games) {
}
