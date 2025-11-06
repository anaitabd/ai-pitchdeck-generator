"""
AI client wrapper for Claude Sonnet 4.5 via Anthropic SDK.
Includes optional Bedrock Runtime support.
"""
import json
import os
from typing import Dict, Optional
from aws_lambda_powertools import Logger
from anthropic import Anthropic
import boto3

logger = Logger(service="ai-client")


class ClaudeClient:
    """
    Client for Claude Sonnet 4.5 AI model via Anthropic SDK.
    Supports both direct Anthropic API and AWS Bedrock.
    """

    def __init__(
        self,
        api_key: Optional[str] = None,
        model: str = "claude-sonnet-4-20250514",
        use_bedrock: bool = False,
        region: str = "us-east-1"
    ):
        """
        Initialize Claude client.

        Args:
            api_key: Anthropic API key (required if not using Bedrock)
            model: Claude model identifier
            use_bedrock: Whether to use AWS Bedrock instead of direct API
            region: AWS region for Bedrock
        """
        self.model = model
        self.use_bedrock = use_bedrock
        self.region = region

        if use_bedrock:
            logger.info(f"Initializing Bedrock Runtime client for region: {region}")
            self.bedrock_client = boto3.client("bedrock-runtime", region_name=region)
            # Map to Bedrock model ID if using Bedrock
            self.bedrock_model_id = self._get_bedrock_model_id(model)
        else:
            logger.info("Initializing Anthropic SDK client")
            api_key = api_key or os.getenv("ANTHROPIC_API_KEY")
            if not api_key:
                raise ValueError("ANTHROPIC_API_KEY is required when not using Bedrock")
            self.client = Anthropic(api_key=api_key)

        logger.info(f"Claude client initialized with model: {model}, bedrock: {use_bedrock}")

    def _get_bedrock_model_id(self, model: str) -> str:
        """
        Map Anthropic model name to Bedrock model ID.

        Args:
            model: Anthropic model identifier

        Returns:
            Bedrock model ID
        """
        bedrock_models = {
            "claude-sonnet-4-20250514": "anthropic.claude-3-5-sonnet-20241022-v2:0",
            "claude-3-5-sonnet-20241022": "anthropic.claude-3-5-sonnet-20241022-v2:0",
            "claude-3-opus-20240229": "anthropic.claude-3-opus-20240229-v1:0",
        }
        return bedrock_models.get(model, "anthropic.claude-3-5-sonnet-20241022-v2:0")

    def generate_pitch_deck(
        self,
        document_content: str,
        system_prompt: Optional[str] = None,
        user_prompt: Optional[str] = None,
        max_tokens: int = 8000,
        temperature: float = 0.7
    ) -> Dict:
        """
        Generate a pitch deck using Claude.

        Args:
            document_content: The input document text
            system_prompt: Optional system prompt to override default
            user_prompt: Optional user prompt to override default
            max_tokens: Maximum tokens to generate
            temperature: Sampling temperature (0.0 to 1.0)

        Returns:
            Dictionary containing generated pitch deck JSON and metadata

        Raises:
            Exception: If generation fails
        """
        logger.info("Starting pitch deck generation with Claude")

        # Build prompts
        final_system_prompt = system_prompt or self._get_default_system_prompt()
        final_user_prompt = self._build_user_prompt(document_content, user_prompt)

        try:
            if self.use_bedrock:
                return self._generate_with_bedrock(
                    final_system_prompt, final_user_prompt, max_tokens, temperature
                )
            else:
                return self._generate_with_anthropic(
                    final_system_prompt, final_user_prompt, max_tokens, temperature
                )
        except Exception as e:
            logger.error("Failed to generate pitch deck", exc_info=True)
            raise

    def _generate_with_anthropic(
        self,
        system_prompt: str,
        user_prompt: str,
        max_tokens: int,
        temperature: float
    ) -> Dict:
        """Generate using Anthropic SDK."""
        logger.info(f"Calling Anthropic API with model: {self.model}")

        response = self.client.messages.create(
            model=self.model,
            max_tokens=max_tokens,
            temperature=temperature,
            system=system_prompt,
            messages=[{"role": "user", "content": user_prompt}]
        )

        logger.info(f"Anthropic API call completed. Usage: {response.usage}")

        # Extract JSON from response
        content = response.content[0].text
        pitch_deck_json = self._extract_json(content)

        return {
            "pitch_deck": pitch_deck_json,
            "input_tokens": response.usage.input_tokens,
            "output_tokens": response.usage.output_tokens,
            "model": self.model
        }

    def _generate_with_bedrock(
        self,
        system_prompt: str,
        user_prompt: str,
        max_tokens: int,
        temperature: float
    ) -> Dict:
        """Generate using AWS Bedrock Runtime."""
        logger.info(f"Calling Bedrock with model: {self.bedrock_model_id}")

        request_body = {
            "anthropic_version": "bedrock-2023-05-31",
            "max_tokens": max_tokens,
            "temperature": temperature,
            "system": system_prompt,
            "messages": [{"role": "user", "content": user_prompt}]
        }

        response = self.bedrock_client.invoke_model(
            modelId=self.bedrock_model_id,
            body=json.dumps(request_body)
        )

        response_body = json.loads(response["body"].read())
        logger.info(f"Bedrock call completed. Usage: {response_body.get('usage', {})}")

        # Extract JSON from response
        content = response_body["content"][0]["text"]
        pitch_deck_json = self._extract_json(content)

        usage = response_body.get("usage", {})
        return {
            "pitch_deck": pitch_deck_json,
            "input_tokens": usage.get("input_tokens"),
            "output_tokens": usage.get("output_tokens"),
            "model": self.bedrock_model_id
        }

    def _extract_json(self, text: str) -> Dict:
        """
        Extract JSON from AI response text.
        Handles markdown code blocks and plain JSON.

        Args:
            text: Response text from AI

        Returns:
            Parsed JSON dictionary
        """
        text = text.strip()

        # Remove markdown code blocks if present
        if text.startswith("```json"):
            text = text[7:]  # Remove ```json
        elif text.startswith("```"):
            text = text[3:]  # Remove ```

        if text.endswith("```"):
            text = text[:-3]  # Remove trailing ```

        text = text.strip()

        try:
            return json.loads(text)
        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse JSON from AI response: {e}")
            logger.debug(f"Response text: {text[:500]}...")
            raise ValueError(f"Invalid JSON in AI response: {e}")

    def _get_default_system_prompt(self) -> str:
        """Get default system prompt for pitch deck generation."""
        return """You are an expert pitch deck consultant with extensive experience helping startups create compelling investor presentations. Your task is to analyze the provided business document and generate a professional pitch deck in JSON format.

Generate a pitch deck with 10-15 slides covering the following structure:
1. Title/Company Overview
2. Problem Statement
3. Solution
4. Market Opportunity
5. Product/Service
6. Business Model
7. Competitive Advantage
8. Go-to-Market Strategy
9. Traction/Milestones
10. Team
11. Financial Projections
12. Funding Ask
13. Vision/Closing

Return your response as a valid JSON object with this exact structure:
{
  "slides": [
    {
      "title": "Slide title here",
      "content": "Detailed slide content here",
      "type": "title|problem|solution|market|product|business_model|competitive|strategy|traction|team|financials|funding|vision"
    }
  ]
}

Ensure the content is concise, compelling, and investor-focused. Each slide should have clear, actionable content."""

    def _build_user_prompt(self, document_content: str, custom_prompt: Optional[str] = None) -> str:
        """Build user prompt with document content."""
        if custom_prompt:
            prompt = custom_prompt + "\n\n"
        else:
            prompt = "Please analyze the following business document and generate a professional pitch deck:\n\n"

        prompt += f"DOCUMENT CONTENT:\n{document_content}\n\n"
        prompt += "Generate the pitch deck in the JSON format specified in the system prompt."

        return prompt
