import { Shape } from '@expo/ui/jetpack-compose';

/**
 * Corner shape for every text field: 16dp, one step under the 20dp grouped cards so fields
 * read as their own element but share the rounded language. Passed to `shape` on
 * `OutlinedTextField`, which otherwise uses Material's 4dp default.
 *
 * Called as a function: expo-ui only accepts the element `Shape.RoundedCorner` returns.
 */
export function fieldShape() {
  return Shape.RoundedCorner({ cornerRadii: { topStart: 16, topEnd: 16, bottomStart: 16, bottomEnd: 16 } });
}
