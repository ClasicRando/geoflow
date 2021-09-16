package orm.enums

/**
 * Enum type found in DB denoting the current state of a run
 * CREATE TYPE public.operation_state AS ENUM
 * ('Ready', 'Active');
 */
enum class OperationState {
    Ready,
    Active,
}