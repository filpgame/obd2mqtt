"""
Dashboard card configurations — pure functions returning Python dicts.
No I/O, no HA dependencies. Fully testable in isolation.
"""
from __future__ import annotations

# ── Constants ────────────────────────────────────────────────────────────────

PERIOD_ENTITY = "input_select.jaecoo_period"
START_ENTITY = "input_datetime.jaecoo_chart_start"
END_ENTITY = "input_datetime.jaecoo_chart_end"

SPEED_ENTITY = "sensor.jaecoo7_jaecoo7_speed"
BATTERY_ENTITY = "sensor.j7_j7_hybrid_battery"
FUEL_ENTITY = "sensor.j7_j7_fuel_level"

PERIOD_OPTIONS = ["Hoje", "7 dias", "30 dias", "Personalizado"]

# Maps input_select option → HA calendar period for statistic card
PERIOD_TO_CALENDAR = {
    "Hoje": "day",
    "7 dias": "week",
    "30 dias": "month",
}

# graph_span Jinja2 template — requires apexcharts-card v2.1+
GRAPH_SPAN_TEMPLATE = (
    "{%- set p = states('" + PERIOD_ENTITY + "') -%}"
    "{%- if p == 'Hoje' -%}24h"
    "{%- elif p == '7 dias' -%}7d"
    "{%- elif p == '30 dias' -%}30d"
    "{%- else -%}"
    "{{ [((as_timestamp(states('" + END_ENTITY + "'))"
    " - as_timestamp(states('" + START_ENTITY + "')))"
    " / 3600) | int, 1] | max }}h"
    "{%- endif -%}"
)


# ── Helper cards ─────────────────────────────────────────────────────────────

def _conditional(state: str, card: dict) -> dict:
    """Wrap a card in a conditional that shows only when PERIOD_ENTITY == state."""
    return {
        "type": "conditional",
        "conditions": [{"entity": PERIOD_ENTITY, "state": state}],
        "card": card,
    }


def _statistic_card(title: str, entity: str, stat_type: str, period: str) -> dict:
    """Built-in statistic card for a single calendar period."""
    return {
        "type": "statistic",
        "entity": entity,
        "stat_type": stat_type,
        "period": {"calendar": {"period": period}},
        "name": title,
    }


# ── Public card builders ──────────────────────────────────────────────────────

def period_selector_card() -> dict:
    """
    Entities card with:
     - input_select dropdown (always visible)
     - start/end input_datetime rows (visible only when Personalizado)
    """
    return {
        "type": "entities",
        "title": "Período",
        "entities": [
            {"entity": PERIOD_ENTITY, "name": "Período"},
            {
                "type": "conditional",
                "conditions": [{"entity": PERIOD_ENTITY, "state": "Personalizado"}],
                "row": {"entity": START_ENTITY, "name": "De"},
            },
            {
                "type": "conditional",
                "conditions": [{"entity": PERIOD_ENTITY, "state": "Personalizado"}],
                "row": {"entity": END_ENTITY, "name": "Até"},
            },
        ],
    }


def summary_stat_column(title: str, entity: str, stat_type: str) -> dict:
    """
    Vertical stack of conditional statistic cards — one per preset period.
    For 'Personalizado' shows current value (statistic unavailable for arbitrary range).
    """
    preset_cards = [
        _conditional(
            label,
            _statistic_card(title, entity, stat_type, calendar_period),
        )
        for label, calendar_period in PERIOD_TO_CALENDAR.items()
    ]
    custom_card = _conditional(
        "Personalizado",
        {"type": "entity", "entity": entity, "name": f"{title} (atual)"},
    )
    return {"type": "vertical-stack", "cards": preset_cards + [custom_card]}


def summary_row() -> dict:
    """Horizontal stack of 3 stat columns: speed avg, fuel change, battery change."""
    return {
        "type": "horizontal-stack",
        "cards": [
            summary_stat_column("Vel. Média", SPEED_ENTITY, "mean"),
            summary_stat_column("Combustível", FUEL_ENTITY, "change"),
            summary_stat_column("Bateria PHEV", BATTERY_ENTITY, "change"),
        ],
    }


def apexcharts_card(title: str, entity: str, color: str, unit: str) -> dict:
    """Area chart with dynamic graph_span driven by period selector."""
    return {
        "type": "custom:apexcharts-card",
        "header": {
            "show": True,
            "title": title,
            "colorize_states": True,
            "show_states": True,
        },
        "update_interval": "30s",
        "graph_span": GRAPH_SPAN_TEMPLATE,
        "apex_config": {
            "chart": {"type": "area"},
            "tooltip": {"x": {"format": "dd/MM HH:mm"}},
            "fill": {"opacity": 0.15},
        },
        "series": [
            {
                "entity": entity,
                "name": title,
                "stroke_width": 2,
                "fill": "tozeroy",
                "color": color,
            }
        ],
        "yaxis": [
            {
                "decimals": 1,
                "apex_config": {"title": {"text": unit}},
            }
        ],
    }


def build_dashboard_config() -> dict:
    """Return the complete Lovelace dashboard configuration dict."""
    return {
        "title": "Carros",
        "views": [
            {
                "title": "Jaecoo 7",
                "path": "jaecoo7",
                "icon": "mdi:car",
                "cards": [
                    period_selector_card(),
                    summary_row(),
                    apexcharts_card(
                        "🔋 Bateria PHEV",
                        BATTERY_ENTITY,
                        "#4CAF50",
                        "%",
                    ),
                    apexcharts_card(
                        "⛽ Combustível",
                        FUEL_ENTITY,
                        "#FF9800",
                        "%",
                    ),
                    apexcharts_card(
                        "🚀 Velocidade",
                        SPEED_ENTITY,
                        "#2196F3",
                        "km/h",
                    ),
                ],
            }
        ],
    }
